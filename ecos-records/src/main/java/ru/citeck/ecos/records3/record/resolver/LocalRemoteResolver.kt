package ru.citeck.ecos.records3.record.resolver

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.RecordsServiceImpl
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.read.AttReadException
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.impl.NullAttValue
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceMeta
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.utils.AttUtils
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LocalRemoteResolver(private val services: RecordsServiceFactory) {

    companion object {
        private val REFS_CACHE_RAW_KEY = "${LocalRemoteResolver::class.simpleName}-refs-cache-raw"
        private val REFS_CACHE_NOT_RAW_KEY = "${LocalRemoteResolver::class.simpleName}-refs-cache"
        private val REFS_CACHE_RAW_SYSTEM_KEY = "${LocalRemoteResolver::class.simpleName}-refs-cache-system-raw"
        private val REFS_CACHE_NOT_RAW_SYSTEM_KEY = "${LocalRemoteResolver::class.simpleName}-refs-system-cache"
    }

    private val emptyAttsMap = AttsMap(emptyMap<String, Any>())

    private val local = services.localRecordsResolver
    private val remote = services.remoteRecordsResolver
    private val reader = services.attSchemaReader

    private val currentAppName = services.properties.appName
    private val defaultAppName = services.properties.defaultApp
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
        return doWithSchema(reader.read(attributes), action)
    }

    private fun <T> doWithSchema(attributes: AttsMap, action: (List<SchemaAtt>) -> T): T {
        return doWithSchema(attributes.getParsedAtts(), action)
    }

    private fun <T> doWithSchema(atts: List<SchemaAtt>, action: (List<SchemaAtt>) -> T): T {
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

        val attsMap = AttsMap(attributes)
        val context: RequestContext = RequestContext.getCurrentNotNull()
        var evaluatedGlobalCtxAtts: ObjectData? = null
        if (!isGatewayMode) {
            val globalCtxAtts = attsMap.extractGlobalCtxAtts(context)
            if (globalCtxAtts.getParsedAtts().isNotEmpty()) {
                doWithSchema(globalCtxAtts) { atts ->
                    val evaluatedAtts = local.getAtts(listOf(NullAttValue.INSTANCE), atts, rawAtts)
                    evaluatedGlobalCtxAtts = evaluatedAtts[0].getAtts()
                }
            }
        }

        val recordObjs = ArrayList<ValWithIdx<Any?>>()
        val recordRefs = ArrayList<ValWithIdx<RecordRef>>()

        for ((idx, rec) in records.withIndex()) {
            if (rec is RecordRef) {
                if (RecordRef.isNotEmpty(rec) || local.hasDaoWithEmptyId()) {
                    recordRefs.add(ValWithIdx(rec, idx))
                } else {
                    recordObjs.add(ValWithIdx(NullAttValue.INSTANCE, idx))
                }
            } else {
                recordObjs.add(ValWithIdx(rec, idx))
            }
        }

        val results = ArrayList<ValWithIdx<RecordAtts>>()

        if (recordObjs.isNotEmpty()) {
            val recordsObjValue = recordObjs.map { it.value }
            val objAtts = doWithSchema(attsMap) { atts ->
                local.getAtts(recordsObjValue, atts, rawAtts)
            }
            if (objAtts.size == recordsObjValue.size) {
                for (i in objAtts.indices) {
                    results.add(ValWithIdx(objAtts[i], recordObjs[i].idx))
                }
            } else {
                error(
                    "Results count doesn't match with " +
                        "requested. objAtts: " + objAtts + " recordsObjValue: " + recordsObjValue
                )
            }
        }

        val refsBySource = RecordsUtils.groupRefBySourceWithIdx(recordRefs)

        refsBySource.forEach { (sourceId, recs) ->
            results.addAll(loadAttsForRefsWithCache(context, sourceId, recs, attsMap, rawAtts))
        }

        val finalGlobalCtxAtts = evaluatedGlobalCtxAtts
        if (finalGlobalCtxAtts != null && finalGlobalCtxAtts.size() > 0) {
            for (record in results) {
                finalGlobalCtxAtts.forEach { key, value ->
                    record.value.setAtt(key, value)
                }
            }
        }

        results.sortBy { it.idx }
        return results.map { it.value }
    }

    private fun loadAttsForRefsWithCache(
        context: RequestContext,
        sourceId: String,
        recs: List<ValWithIdx<RecordRef>>,
        attsMap: AttsMap,
        rawAtts: Boolean
    ): List<ValWithIdx<RecordAtts>> {

        if (!context.ctxData.readOnly) {
            return loadAttsForRefs(context, sourceId, recs, attsMap, rawAtts)
        }

        val cacheKey = if (rawAtts) {
            if (AuthContext.isRunAsSystem()) {
                REFS_CACHE_RAW_SYSTEM_KEY
            } else {
                REFS_CACHE_RAW_KEY
            }
        } else {
            if (AuthContext.isRunAsSystem()) {
                REFS_CACHE_NOT_RAW_SYSTEM_KEY
            } else {
                REFS_CACHE_NOT_RAW_KEY
            }
        }
        val recordsCache: MutableMap<RecordRef, MutableMap<String, DataValue>> = context.getReadOnlyCache(cacheKey)
        val cachedAttsWithAliases: MutableMap<String, String> = HashMap(
            attsMap.getAttributes().entries.mapNotNull {
                val value = it.value
                if (value !is String) {
                    null
                } else {
                    it.key to value
                }
            }.toMap()
        )

        if (cachedAttsWithAliases.isEmpty()) {
            return loadAttsForRefs(context, sourceId, recs, attsMap, rawAtts)
        }

        val cacheByRecord = recs.map { recordsCache.computeIfAbsent(it.value) { HashMap() } }
        val cachedAttValues = recs.map { HashMap<String, DataValue>() }
        val notCachedAtts = hashSetOf<String>()

        for ((idx, cache) in cacheByRecord.withIndex()) {
            val cachedRecordAttValues = cachedAttValues[idx]
            notCachedAtts.clear()
            cachedAttsWithAliases.forEach { (alias, attribute) ->
                // context attributes should not be cached
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
            attsMap.getAttributes()
        } else {
            attsMap.getAttributes().filter { cachedAttsWithAliases[it.key] == null }.toMap()
        }

        val loadedAtts = loadAttsForRefs(context, sourceId, recs, AttsMap(attsToLoad), rawAtts)
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
        attsMap: AttsMap,
        rawAtts: Boolean
    ): List<ValWithIdx<RecordAtts>> {

        if (recs.isEmpty()) {
            return emptyList()
        }
        if (attsMap.getAttributes().isEmpty()) {
            return recs.map { ValWithIdx(RecordAtts(it.value), it.idx) }
        }

        val refs: List<RecordRef> = recs.map { it.value }

        val atts: List<RecordAtts> = if (!isGatewayMode && !isRemoteSourceId(sourceId)) {
            doWithSchema(attsMap) { schema -> local.getAtts(refs, schema, rawAtts) }
        } else if (remote != null && (isGatewayMode || isRemoteRef(recs.map { it.value }.firstOrNull()))) {
            remote.getAtts(refs, attsMap.getAttributes(), rawAtts)
        } else {
            doWithSchema(attsMap) { schema -> local.getAtts(refs, schema, rawAtts) }
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

    fun mutateForAllApps(records: List<RecordAtts>, attsToLoad: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {

        if (records.isEmpty()) {
            return emptyList()
        }

        val recsToMutate = ArrayList<ValWithIdx<RecordAtts>>()
        val allRecsAfterMutate = ArrayList<ValWithIdx<RecordAtts>>()
        val refsByAliases = HashMap<String, RecordRef>()

        var appToMutate = ""

        val flushRecords = {

            for (record in recsToMutate) {
                convertAssocValues(record.value, refsByAliases)
            }
            recsToMutate.reverse()
            val recsAfterMutate = mutateForApp(
                appToMutate == currentAppName,
                recsToMutate.map { it.value },
                attsToLoad,
                rawAtts
            )

            for ((idx, atts) in recsAfterMutate.withIndex()) {
                val recToMutateWithIdx = recsToMutate[idx]
                val alias = findAliasInRawAttsToMutate(recToMutateWithIdx.value.getAtts())
                if (alias.isNotBlank()) {
                    refsByAliases[alias] = atts.getId()
                }
                allRecsAfterMutate.add(ValWithIdx(atts, recToMutateWithIdx.idx))
            }
            appToMutate = ""
            recsToMutate.clear()
        }

        for (i in records.indices.reversed()) {
            val record = records[i]
            val appName = getTargetAppName(record.getId())
            if (appToMutate.isEmpty() || appName == appToMutate) {
                appToMutate = appName
                recsToMutate.add(ValWithIdx(record, i))
                // we should not batch local records for correct
                // working of convertAssocValues function
                if (appToMutate == currentAppName) {
                    flushRecords()
                }
            } else {
                flushRecords()
                appToMutate = appName
                recsToMutate.add(ValWithIdx(record, i))
            }
        }
        if (recsToMutate.isNotEmpty()) {
            flushRecords()
        }

        allRecsAfterMutate.sortBy { it.idx }
        return allRecsAfterMutate.map { it.value }
    }

    private fun mutateForApp(
        isLocalApp: Boolean,
        records: List<RecordAtts>,
        attsToLoad: Map<String, *>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        if (records.isEmpty()) {
            return emptyList()
        }

        val result = if (!isLocalApp) {
            remote ?: error(
                "RemoteRecordsResolver is null. " +
                    "Remote records can't be mutated: ${records.map { it.getId() }}"
            )
            remote.mutate(records, attsToLoad, rawAtts)
        } else {
            doWithSchema(attsToLoad) { schema -> local.mutate(records, schema, rawAtts) }
        }
        return result
    }

    private fun getTargetAppName(ref: RecordRef): String {
        return if (ref.appName.isEmpty()) {
            if (isGatewayMode) {
                defaultAppName
            } else {
                currentAppName
            }
        } else {
            val sourceId = ref.appName + RecordRef.APP_NAME_DELIMITER + ref.sourceId
            if (local.containsDao(sourceId)) {
                currentAppName
            } else {
                ref.appName
            }
        }
    }

    private fun findAliasInRawAttsToMutate(rawAtts: ObjectData): String {
        if (rawAtts.size() == 0) {
            return ""
        }
        for (field in rawAtts.fieldNames()) {
            if (field.startsWith(RecordConstants.ATT_ALIAS)) {
                if (RecordConstants.ATT_ALIAS == field.substringBefore('?')) {
                    return rawAtts.get(field, "")
                }
            }
        }
        return ""
    }

    private fun convertAssocValues(record: RecordAtts, assocsMapping: Map<String, RecordRef>) {

        val recAtts = ObjectData.create()

        record.forEach { name, valueArg ->
            try {
                val parsedAtt = reader.read("", name)
                recAtts.set(parsedAtt.name, convertAssocValue(valueArg, assocsMapping))
            } catch (e: AttReadException) {
                RecordsServiceImpl.log.error("Attribute read failed", e)
            }
        }
        record.setAtts(recAtts)
    }

    private fun convertAssocValue(value: DataValue, mapping: Map<String, RecordRef>): DataValue {
        if (mapping.isEmpty()) {
            return value
        }
        if (value.isTextual()) {
            val textValue: String = value.asText()
            if (mapping.containsKey(textValue)) {
                return DataValue.create(mapping[textValue].toString())
            }
        } else if (value.isArray()) {
            val convertedValue: MutableList<DataValue?> = ArrayList()
            for (node in value) {
                convertedValue.add(convertAssocValue(node, mapping))
            }
            return DataValue.create(convertedValue)
        }
        return value
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
        doWithGroupOfRemoteOrLocalInAnyOrder(recordRefs) { refs, isRemote ->
            if (isRemote) {
                remote?.commit(refs)
            } else {
                local.commit(refs)
            }
        }
    }

    fun rollback(recordRefs: List<RecordRef>) {
        doWithGroupOfRemoteOrLocalInAnyOrder(recordRefs) { refs, isRemote ->
            if (isRemote) {
                remote?.rollback(refs)
            } else {
                local.rollback(refs)
            }
        }
    }

    private fun doWithGroupOfRemoteOrLocalInAnyOrder(
        recordRefs: List<RecordRef>,
        action: (List<RecordRef>, Boolean) -> Unit
    ) {
        if (recordRefs.isEmpty()) {
            return
        }
        if (remote == null) {
            action.invoke(recordRefs, false)
            return
        }
        val remoteRecs = mutableListOf<RecordRef>()
        val localRecs = mutableListOf<RecordRef>()
        recordRefs.forEach {
            if (isRemoteRef(it)) {
                remoteRecs.add(it)
            } else {
                localRecs.add(it)
            }
        }
        if (localRecs.isNotEmpty()) {
            action.invoke(localRecs, false)
        }
        if (remoteRecs.isNotEmpty()) {
            action.invoke(remoteRecs, true)
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

    /**
     * Class to cache attributes parsing result
     */
    private inner class AttsMap(attributes: Map<String, *>) {

        private var attributes: MutableMap<String, *> = LinkedHashMap(attributes)
        private var parsedAtts: MutableList<SchemaAtt>? = null

        fun getAttributes(): Map<String, *> {
            return attributes
        }

        fun getParsedAtts(): MutableList<SchemaAtt> {
            val currentAtts = parsedAtts
            if (currentAtts != null) {
                return currentAtts
            }
            val parsedAtts = ArrayList(reader.read(attributes))
            this.parsedAtts = parsedAtts
            return parsedAtts
        }

        /**
         * Extract (get and remove) global context attributes
         */
        fun extractGlobalCtxAtts(context: RequestContext): AttsMap {

            if (attributes.isEmpty()) {
                return emptyAttsMap
            }
            val ctxAtts = context.getCtxAtts()
            if (ctxAtts.isEmpty()) {
                return emptyAttsMap
            }
            val newParsedAtts = ArrayList<SchemaAtt>()
            val newAttsMap = LinkedHashMap<String, Any?>()

            val it = getParsedAtts().listIterator()
            while (it.hasNext()) {
                val att = it.next()
                if (AttUtils.isGlobalContextAtt(att, ctxAtts)) {
                    newParsedAtts.add(att)
                    val alias = att.getAliasForValue()
                    newAttsMap[alias] = attributes.remove(alias)
                    it.remove()
                }
            }

            val newAtts = AttsMap(newAttsMap)
            newAtts.parsedAtts = newParsedAtts
            return newAtts
        }
    }
}
