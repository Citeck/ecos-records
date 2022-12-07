package ru.citeck.ecos.records3.record.resolver

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.RecordsServiceImpl
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.read.AttReadException
import ru.citeck.ecos.records3.record.atts.schema.read.AttSchemaReader
import ru.citeck.ecos.records3.record.atts.value.impl.NullAttValue
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceMeta
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.utils.AttUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LocalRemoteResolver(services: RecordsServiceFactory) : ServiceFactoryAware {

    companion object {
        private val REFS_CACHE_RAW_KEY = "${LocalRemoteResolver::class.simpleName}-refs-cache-raw"
        private val REFS_CACHE_NOT_RAW_KEY = "${LocalRemoteResolver::class.simpleName}-refs-cache"
        private val REFS_CACHE_RAW_SYSTEM_KEY = "${LocalRemoteResolver::class.simpleName}-refs-cache-system-raw"
        private val REFS_CACHE_NOT_RAW_SYSTEM_KEY = "${LocalRemoteResolver::class.simpleName}-refs-system-cache"
    }

    private val emptyAttsMap = AttsMap(emptyMap<String, Any>())

    private lateinit var local: LocalRecordsResolver
    private var remote: RemoteRecordsResolver? = null
    private lateinit var reader: AttSchemaReader

    private val currentAppName = services.webappProps.appName
    private val defaultAppName = services.properties.defaultApp
    private val currentAppSourceIdPrefix = "$currentAppName/"
    private val isGatewayMode = services.webappProps.gatewayMode
    private val legacyApiMode = services.properties.legacyApiMode

    private val virtualRecords = ConcurrentHashMap<EntityRef, Any>()

    fun query(query: RecordsQuery, attributes: Map<String, *>, rawAtts: Boolean): RecsQueryRes<RecordAtts> {
        val sourceId = query.sourceId
        val remote = this.remote
        return if (remote == null || !isGatewayMode && !isRemoteSourceId(sourceId)) {
            local.queryRecords(query.withSourceId(getLocalSourceId(sourceId)), reader.read(attributes), rawAtts)
        } else {
            remote.query(query, attributes, rawAtts)
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
                val evaluatedAtts = local.getValuesAtts(
                    listOf(NullAttValue.INSTANCE),
                    globalCtxAtts.getParsedAtts(),
                    rawAtts
                )
                evaluatedGlobalCtxAtts = evaluatedAtts[0].getAtts()
            }
        }

        val recordObjs = ArrayList<ValWithIdx<Any?>>()
        val recordRefs = ArrayList<ValWithIdx<RecordRef>>()

        for ((idx, rec) in records.withIndex()) {
            val fixedRef = if (rec is EntityRef && rec !is RecordRef) {
                RecordRef.create(rec.getAppName(), rec.getSourceId(), rec.getLocalId())
            } else {
                rec
            }
            if (fixedRef is RecordRef) {
                val virtualRec = virtualRecords[fixedRef.withDefaultAppName(currentAppName)]
                if (virtualRec != null) {
                    recordObjs.add(ValWithIdx(virtualRec, idx))
                } else {
                    if (RecordRef.isNotEmpty(fixedRef) || local.hasDaoWithEmptyId()) {
                        if (virtualRecords.contains(fixedRef)) {
                            recordObjs.add(ValWithIdx(fixedRef, idx))
                        } else {
                            recordRefs.add(ValWithIdx(fixedRef, idx))
                        }
                    } else {
                        recordObjs.add(ValWithIdx(NullAttValue.INSTANCE, idx))
                    }
                }
            } else {
                recordObjs.add(ValWithIdx(fixedRef, idx))
            }
        }

        val results = ArrayList<ValWithIdx<RecordAtts>>()

        if (recordObjs.isNotEmpty()) {
            val recordsObjValue = recordObjs.map { it.value }
            val objAtts = local.getValuesAtts(recordsObjValue, attsMap.getParsedAtts(), rawAtts)
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

        val remote = this.remote
        val atts: List<RecordAtts> = if (remote != null && (isGatewayMode || isRemoteSourceId(sourceId))) {
            remote.getAtts(refs, attsMap.getAttributes(), rawAtts)
        } else {
            local.getRecordsAtts(getLocalSourceId(sourceId), refs.map { it.id }, attsMap.getParsedAtts(), rawAtts)
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

    fun mutateForAllApps(records: List<RecordAtts>, attsToLoad: List<Map<String, *>>, rawAtts: Boolean): List<RecordAtts> {

        if (records.isEmpty()) {
            return emptyList()
        }

        val recsToMutate = ArrayList<ValWithIdx<RecordAtts>>()
        val recsAttsToLoad = ArrayList<Map<String, *>>()

        val allRecsAfterMutate = ArrayList<ValWithIdx<RecordAtts>>()
        val refsByAliases = HashMap<String, RecordRef>()

        var appToMutate = ""

        val flushRecords = {

            for (record in recsToMutate) {
                convertAssocValues(record.value, refsByAliases)
            }
            recsToMutate.reverse()
            recsAttsToLoad.reverse()
            val recsAfterMutate = mutateForApp(
                appToMutate == currentAppName,
                recsToMutate.map { it.value },
                recsAttsToLoad,
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
            recsAttsToLoad.clear()
        }

        for (i in records.indices.reversed()) {
            val record = records[i]
            val appName = getTargetAppName(record.getId())
            if (appToMutate.isEmpty() || (appName == appToMutate && !legacyApiMode)) {
                appToMutate = appName
                recsToMutate.add(ValWithIdx(record, i))
                recsAttsToLoad.add(attsToLoad.getOrNull(i) ?: emptyMap<String, Any>())
                // we should not batch local records for correct
                // working of convertAssocValues function
                if (appToMutate == currentAppName) {
                    flushRecords()
                }
            } else {
                flushRecords()
                appToMutate = appName
                recsToMutate.add(ValWithIdx(record, i))
                recsAttsToLoad.add(attsToLoad.getOrNull(i) ?: emptyMap<String, Any>())
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
        attsToLoad: List<Map<String, *>>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        if (records.isEmpty()) {
            return emptyList()
        }

        val result = if (!isLocalApp) {
            val remote = remote ?: error(
                "RemoteRecordsResolver is null. " +
                    "Remote records can't be mutated: ${records.map { it.getId() }}"
            )
            remote.mutate(records, attsToLoad, rawAtts)
        } else {
            val parsedAttsToLoad = attsToLoad.map { reader.read(it) }
            val result = mutableListOf<RecordAtts>()

            for ((idx, record) in records.withIndex()) {
                val sourceId = getLocalSourceId(record.getId())
                val attsToLoadForRecord = parsedAttsToLoad.getOrNull(idx) ?: emptyList()
                result.add(
                    local.mutateRecord(
                        sourceId,
                        LocalRecordAtts(record.getId().id, record.getAtts()),
                        attsToLoadForRecord,
                        rawAtts
                    )
                )
            }
            result
        }
        return result
    }

    private fun getLocalSourceId(ref: EntityRef): String {
        val app = ref.getAppName()
        val srcId = ref.getSourceId()
        return if (app.isNotBlank() && app != currentAppName) {
            app + EntityRef.APP_NAME_DELIMITER + srcId
        } else {
            srcId
        }
    }

    private fun getLocalSourceId(sourceId: String): String {
        if (!sourceId.contains(EntityRef.APP_NAME_DELIMITER)) {
            return sourceId
        }
        if (sourceId.startsWith(currentAppSourceIdPrefix)) {
            return sourceId.substring(currentAppSourceIdPrefix.length)
        }
        return sourceId
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
                recAtts[parsedAtt.name] = convertAssocValue(valueArg, assocsMapping)
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

    fun delete(records: List<EntityRef>): List<DelStatus> {
        if (records.isEmpty()) {
            return emptyList()
        }
        val remote = remote
        return if (remote != null && (isGatewayMode || isRemoteRef(records[0]))) {
            remote.delete(records)
        } else {
            doWithGroupsInSameOrder(records, { r0, r1, _, _ ->
                r0.getAppName() == r1.getAppName() &&
                    r0.getSourceId() == r1.getSourceId()
            }) { refs, _ ->
                local.deleteRecords(getLocalSourceId(refs[0]), refs.map { ref -> ref.getLocalId() })
            }
        }
    }

    private inline fun <T, R> doWithGroupsInSameOrder(
        elements: Iterable<T>,
        compare: (T, T, idx0: Int, idx1: Int) -> Boolean,
        crossinline action: (List<T>, offset: Int) -> List<R>
    ): List<R> {
        val iterator = elements.iterator()
        if (!iterator.hasNext()) {
            return emptyList()
        }
        val results = ArrayList<R>()
        val groupList = ArrayList<T>(16)
        groupList.add(iterator.next())
        var idx0 = 0
        var idxLast = 1
        while (iterator.hasNext()) {
            val nextElement = iterator.next()
            if (compare.invoke(groupList[0], nextElement, idx0, idxLast)) {
                groupList.add(nextElement)
            } else {
                results.addAll(action.invoke(groupList, idx0))
                groupList.clear()
                groupList.add(nextElement)
                idx0 = idxLast
            }
            idxLast++
        }
        if (groupList.isNotEmpty()) {
            results.addAll(action.invoke(groupList, idx0))
        }
        return results
    }

    fun isSourceTransactional(sourceId: String): Boolean {
        val remote = remote
        if (isRemoteSourceId(sourceId) && remote != null) {
            return remote.isSourceTransactional(sourceId)
        }
        return local.isSourceTransactional(getLocalSourceId(sourceId))
    }

    fun commit(recordRefs: List<RecordRef>) {
        commit(recordRefs, false)
    }

    fun commit(recordRefs: List<RecordRef>, skipRemote: Boolean) {
        doWithGroupOfRemoteOrLocalInAnyOrder(recordRefs) { refs, isRemote ->
            if (isRemote) {
                if (!skipRemote) {
                    remote?.commit(refs)
                }
            } else {
                doWithGroupsInSameOrder(recordRefs, { r0, r1, _, _ ->
                    r0.appName == r1.appName && r0.sourceId == r1.sourceId
                }) { groupedRefs, _ ->
                    local.commit(getLocalSourceId(groupedRefs[0]), groupedRefs.map { it.getLocalId() })
                    emptyList<Unit>()
                }
            }
        }
    }

    fun rollback(recordRefs: List<RecordRef>) {
        return rollback(recordRefs, false)
    }

    fun rollback(recordRefs: List<RecordRef>, skipRemote: Boolean) {
        doWithGroupOfRemoteOrLocalInAnyOrder(recordRefs) { refs, isRemote ->
            if (isRemote) {
                if (!skipRemote) {
                    remote?.rollback(refs)
                }
            } else {
                doWithGroupsInSameOrder(recordRefs, { r0, r1, _, _ ->
                    r0.appName == r1.appName && r0.sourceId == r1.sourceId
                }) { groupedRefs, _ ->

                    local.rollback(
                        getLocalSourceId(groupedRefs[0]),
                        groupedRefs.map { it.getLocalId() }
                    )
                    emptyList<Unit>()
                }
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
            local.getSourceInfo(getLocalSourceId(sourceId))
        }
    }

    fun getSourcesInfo(): List<RecordsSourceMeta> {
        val result = ArrayList(local.getSourcesInfo())
        result.addAll(remote?.getSourcesInfo() ?: emptyList())
        return result
    }

    private fun isRemoteRef(ref: EntityRef?): Boolean {
        return ref != null &&
            ref.getAppName().isNotEmpty() &&
            isRemoteSourceId(ref.getAppName() + "/" + ref.getSourceId())
    }

    private fun isRemoteSourceId(sourceId: String?): Boolean {
        if (sourceId == null || StringUtils.isBlank(sourceId)) {
            return false
        }
        return if (local.containsDao(getLocalSourceId(sourceId))) {
            false
        } else {
            sourceId.contains("/") && !sourceId.startsWith(currentAppSourceIdPrefix)
        }
    }

    fun register(sourceId: String, recordsDao: RecordsDao) {
        local.register(getLocalSourceId(sourceId), recordsDao)
    }

    fun unregister(sourceId: String) {
        local.unregister(getLocalSourceId(sourceId))
    }

    fun <T : Any> getRecordsDao(sourceId: String, type: Class<T>): T? {
        return local.getRecordsDao(getLocalSourceId(sourceId), type)
    }

    fun setRecordsService(serviceFactory: RecordsService) {
        remote?.setRecordsService(serviceFactory)
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        local = serviceFactory.localRecordsResolver
        remote = serviceFactory.remoteRecordsResolver
        reader = serviceFactory.attSchemaReader
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

    fun containsVirtualRecord(ref: EntityRef): Boolean {
        return virtualRecords.contains(ref.withDefaultAppName(currentAppName))
    }

    fun registerVirtualRecord(ref: EntityRef, value: Any) {
        virtualRecords[ref.withDefaultAppName(currentAppName)] = value
    }

    fun unregisterVirtualRecord(ref: EntityRef) {
        virtualRecords.remove(ref.withDefaultAppName(currentAppName))
    }
}
