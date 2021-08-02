package ru.citeck.ecos.records3.record.resolver

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceMeta
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LocalRemoteResolver(private val services: RecordsServiceFactory) {

    companion object {
        private val REFS_CACHE_RAW_KEY = "${LocalRemoteResolver::class.simpleName}-refs-cache-raw"
        private val REFS_CACHE_NOT_RAW_KEY = "${LocalRemoteResolver::class.simpleName}-refs-cache"
    }

    private val local = services.localRecordsResolver
    private val remote = services.remoteRecordsResolver
    private val reader = services.attSchemaReader

    private val currentAppSourceIdPrefix = services.properties.appName + "/"
    private val isGatewayMode = services.properties.gatewayMode

    fun query(query: RecordsQuery, attributes: Map<String, *>, rawAtts: Boolean): RecsQueryRes<RecordAtts> {
        val sourceId = query.sourceId
        return if (remote == null || !isGatewayMode && !isRemoteSourceId(sourceId)) {
            doWithSchema(attributes) { schema -> local.query(query, schema, rawAtts) }
        } else {
            remote.query(query, attributes, rawAtts)
        }
    }

    private fun <T> doWithSchema(attributes: Map<String, *>, action: (List<SchemaAtt>) -> T): T {
        val atts: List<SchemaAtt> = reader.read(attributes)
        return AttContext.doWithCtx(services) { attContext ->
            val schemaAttBefore = attContext.getSchemaAtt()
            val attPathBefore = attContext.getAttPath()
            if (atts.isNotEmpty()) {
                attContext.setSchemaAtt(
                    SchemaAtt.create()
                        .withName(SchemaAtt.ROOT_NAME)
                        .withInner(atts)
                        .build()
                )
                attContext.setAttPath("")
            }
            try {
                action.invoke(atts)
            } finally {
                attContext.setSchemaAtt(schemaAttBefore)
                attContext.setAttPath(attPathBefore)
            }
        }
    }

    fun getAtts(rawRecords: List<*>, attributes: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {

        if (rawRecords.isEmpty()) {
            return emptyList()
        }
        val records = rawRecords.map {
            if (it is String) {
                RecordRef.valueOf(it)
            } else {
                it
            }
        }

        val context: RequestContext = RequestContext.getCurrentNotNull()
        val recordObjs = ArrayList<ValWithIdx<Any?>>()
        val recordRefs = ArrayList<ValWithIdx<RecordRef>>()

        for ((idx, rec) in records.withIndex()) {
            if (rec is RecordRef) {
                recordRefs.add(ValWithIdx(rec, idx))
            } else {
                recordObjs.add(ValWithIdx(rec, idx))
            }
        }

        val results = ArrayList<ValWithIdx<RecordAtts>>()
        val recordsObjValue = recordObjs.map { it.value }
        val objAtts = doWithSchema(attributes) { atts ->
            local.getAtts(recordsObjValue, atts, rawAtts)
        }
        if (objAtts.size == recordsObjValue.size) {
            for (i in objAtts.indices) {
                results.add(ValWithIdx(objAtts[i], recordObjs[i].idx))
            }
        } else {
            context.addMsg(MsgLevel.ERROR) {
                "Results count doesn't match with " +
                    "requested. objAtts: " + objAtts + " recordsObjValue: " + recordsObjValue
            }
            return recordsObjValue.map { RecordAtts() }
        }

        val refsBySource = RecordsUtils.groupRefBySourceWithIdx(recordRefs)

        refsBySource.forEach { (sourceId, recs) ->
            results.addAll(loadAttsForRefsWithCache(context, sourceId, recs, attributes, rawAtts))
        }
        results.sortBy { it.idx }
        return results.map { it.value }
    }

    private fun loadAttsForRefsWithCache(
        context: RequestContext,
        sourceId: String,
        recs: List<ValWithIdx<RecordRef>>,
        attributes: Map<String, *>,
        rawAtts: Boolean
    ): List<ValWithIdx<RecordAtts>> {
        if (!context.ctxData.readOnly) {
            return loadAttsForRefs(context, sourceId, recs, attributes, rawAtts)
        }

        val cacheKey = if (rawAtts) {
            REFS_CACHE_RAW_KEY
        } else {
            REFS_CACHE_NOT_RAW_KEY
        }
        val recordsCache: MutableMap<RecordRef, MutableMap<String, DataValue>> = context.getMap(cacheKey)
        val cachedAttsWithAliases: MutableMap<String, String> = HashMap(
            attributes.entries.mapNotNull {
                val value = it.value
                if (value !is String) {
                    null
                } else {
                    it.key to value
                }
            }.toMap()
        )

        if (cachedAttsWithAliases.isEmpty()) {
            return loadAttsForRefs(context, sourceId, recs, attributes, rawAtts)
        }

        val cacheByRecord = recs.map { recordsCache.computeIfAbsent(it.value) { HashMap() } }
        val cachedAttValues = recs.map { HashMap<String, DataValue>() }
        val notCachedAtts = hashSetOf<String>()

        for ((idx, cache) in cacheByRecord.withIndex()) {
            val cachedRecordAttValues = cachedAttValues[idx]
            notCachedAtts.clear()
            cachedAttsWithAliases.forEach { (alias, attribute) ->
                val valueFromCache = if (!attribute.startsWith("$")) {
                    cache[attribute]
                } else {
                    null
                }
                if (valueFromCache == null) {
                    notCachedAtts.add(alias)
                } else {
                    cachedRecordAttValues[alias] = valueFromCache
                }
            }
            notCachedAtts.forEach { cachedAttsWithAliases.remove(it) }
            if (cachedAttsWithAliases.isNotEmpty()) {
                break
            }
        }

        val attsToLoad = if (cachedAttsWithAliases.isEmpty()) {
            attributes
        } else {
            attributes.filter { cachedAttsWithAliases[it.key] == null }.toMap()
        }

        val loadedAtts = loadAttsForRefs(context, sourceId, recs, attsToLoad, rawAtts)
        loadedAtts.forEachIndexed { idx, recordAttsWithIdx ->

            val recAtts = recordAttsWithIdx.value
            val recCache = cacheByRecord[idx]

            recAtts.getAtts().forEach { key, value ->
                recCache[key] = value
            }

            val cachedRecordAttValues = cachedAttValues[idx]
            cachedRecordAttValues.forEach { (alias, value) ->
                recAtts.setAtt(alias, value)
            }
        }
        return loadedAtts
    }

    private fun loadAttsForRefs(
        context: RequestContext,
        sourceId: String,
        recs: List<ValWithIdx<RecordRef>>,
        attributes: Map<String, *>,
        rawAtts: Boolean
    ): List<ValWithIdx<RecordAtts>> {

        val refs: List<RecordRef> = recs.map { it.value }

        val atts: List<RecordAtts> = if (!isGatewayMode && !isRemoteSourceId(sourceId)) {
            doWithSchema(attributes) { schema -> local.getAtts(refs, schema, rawAtts) }
        } else if (remote != null && (isGatewayMode || isRemoteRef(recs.map { it.value }.firstOrNull()))) {
            remote.getAtts(refs, attributes, rawAtts)
        } else {
            doWithSchema(attributes) { schema -> local.getAtts(refs, schema, rawAtts) }
        }
        return if (atts.size != refs.size) {
            context.addMsg(MsgLevel.ERROR) {
                "Results count doesn't match with " +
                    "requested. Atts: " + atts + " refs: " + refs
            }
            recs.map { record -> ValWithIdx(RecordAtts(record.value), record.idx) }
        } else {
            refs.indices.map { idx -> ValWithIdx(atts[idx], recs[idx].idx) }
        }
    }

    fun mutate(records: List<RecordAtts>): List<RecordRef> {
        if (records.isEmpty()) {
            return emptyList()
        }
        if (remote == null) {
            return local.mutate(records)
        }
        return if (isGatewayMode || isRemoteRef(records[0])) {
            remote.mutate(records)
        } else {
            local.mutate(records)
        }
    }

    fun delete(records: List<RecordRef>): List<DelStatus> {
        if (records.isEmpty()) {
            return emptyList()
        }
        if (remote == null) {
            return local.delete(records)
        }
        return if (isGatewayMode || isRemoteRef(records[0])) {
            remote.delete(records)
        } else {
            local.delete(records)
        }
    }

    fun isSourceTransactional(sourceId: String): Boolean {
        if (isRemoteSourceId(sourceId) && remote != null) {
            return remote.isSourceTransactional(sourceId)
        }
        return local.isSourceTransactional(sourceId)
    }

    fun commit(recordRefs: List<RecordRef>) {
        doWithGroupOfRemoteOrLocal(recordRefs) { refs, isRemote ->
            if (isRemote) {
                remote?.commit(refs)
            } else {
                local.commit(refs)
            }
        }
    }

    fun rollback(recordRefs: List<RecordRef>) {
        doWithGroupOfRemoteOrLocal(recordRefs) { refs, isRemote ->
            if (isRemote) {
                remote?.rollback(refs)
            } else {
                local.rollback(refs)
            }
        }
    }

    private fun doWithGroupOfRemoteOrLocal(recordRefs: List<RecordRef>, action: (List<RecordRef>, Boolean) -> Unit) {
        if (recordRefs.isEmpty()) {
            return
        }
        if (remote == null) {
            action.invoke(recordRefs, false)
            return
        }
        var idx = 1
        var isRemote = isRemoteRef(recordRefs[0])
        val refs = ArrayList<RecordRef>()
        refs.add(recordRefs[0])
        while (idx < recordRefs.size) {
            val nextRef = recordRefs[idx++]
            val isNextRefRemote = isRemoteRef(nextRef)
            if (isNextRefRemote != isRemote) {
                action.invoke(refs, isRemote)
                refs.clear()
                refs.add(nextRef)
                isRemote = isNextRefRemote
            }
        }
        if (refs.isNotEmpty()) {
            action.invoke(refs, isRemote)
        }
    }

    fun getSourceInfo(sourceId: String): RecordsSourceMeta? {
        return if (isGatewayMode || isRemoteSourceId(sourceId)) {
            remote?.getSourceInfo(sourceId)
        } else {
            local.getSourceInfo(sourceId)
        }
    }

    fun getSourcesInfo(): List<RecordsSourceMeta> {
        val result = ArrayList(local.getSourcesInfo())
        result.addAll(remote?.getSourcesInfo() ?: emptyList())
        return result
    }

    private fun isRemoteRef(meta: RecordAtts): Boolean {
        return isRemoteRef(meta.getId())
    }

    private fun isRemoteRef(ref: RecordRef?): Boolean {
        return ref != null && ref.isRemote() && isRemoteSourceId(ref.appName + "/" + ref.sourceId)
    }

    private fun isRemoteSourceId(sourceId: String?): Boolean {
        if (sourceId == null || StringUtils.isBlank(sourceId)) {
            return false
        }
        return if (local.containsDao(sourceId)) {
            false
        } else {
            sourceId.contains("/") && !sourceId.startsWith(currentAppSourceIdPrefix)
        }
    }

    fun register(sourceId: String, recordsDao: RecordsDao) {
        local.register(sourceId, recordsDao)
    }

    fun unregister(sourceId: String) {
        local.unregister(sourceId)
    }

    fun <T : Any> getRecordsDao(sourceId: String, type: Class<T>): T? {
        return local.getRecordsDao(sourceId, type)
    }

    fun setRecordsService(serviceFactory: RecordsService) {
        remote?.setRecordsService(serviceFactory)
    }
}
