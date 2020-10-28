package ru.citeck.ecos.records3.record.resolver

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.RecordsServiceFactory
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.*
import java.util.function.Function

class LocalRemoteResolver(private val services: RecordsServiceFactory) {

    private val local = services.localRecordsResolver
    private val remote = services.remoteRecordsResolver
    private val reader = services.attSchemaReader
    private val currentAppSourceIdPrefix: String
    private val isGatewayMode: Boolean

    init {
        currentAppSourceIdPrefix = services.properties.appName + "/"
        isGatewayMode = services.properties.isGatewayMode
    }

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
            if (atts.isNotEmpty()) {
                attContext.schemaAtt = SchemaAtt.create()
                    .withName("")
                    .withInner(atts)
                    .build()
            }
            action.invoke(atts)
        }
    }

    fun getAtts(records: List<*>, attributes: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {

        if (records.isEmpty()) {
            return emptyList()
        }
        if (remote == null) {
            return doWithSchema(attributes) { atts -> local.getAtts(records, atts, rawAtts) }
        }
        val context: RequestContext = RequestContext.Companion.getCurrentNotNull()
        val recordObjs: List<ValWithIdx<Any>> = ArrayList<ValWithIdx<Any>>()
        val recordRefs: List<ValWithIdx<RecordRef>> = ArrayList<ValWithIdx<RecordRef>>()
        var idx = 0
        for (rec in records) {
            if (rec is RecordRef) {
                recordRefs.add(ValWithIdx<RecordRef?>(rec as RecordRef?, idx))
            } else {
                recordObjs.add(ValWithIdx<Any?>(rec, idx))
            }
            idx++
        }
        val results: List<ValWithIdx<RecordAtts>> = ArrayList<ValWithIdx<RecordAtts>>()
        val recordsObjValue = recordObjs.map { it.value }
        val objAtts: List<RecordAtts> = doWithSchema(attributes) { atts ->
            local.getAtts(recordsObjValue, atts, rawAtts)
        }
        if (objAtts != null && objAtts.size == recordsObjValue!!.size) {
            for (i in objAtts.indices) {
                results.add(ValWithIdx<RecordAtts?>(objAtts[i], recordObjs[i].getIdx()))
            }
        } else {
            context.addMsg(MsgLevel.ERROR) {
                "Results count doesn't match with " +
                    "requested. objAtts: " + objAtts + " recordsObjValue: " + recordsObjValue
            }
            return null
        }
        RecordsUtils.groupRefBySourceWithIdx(recordRefs).forEach(BiConsumer<String, List<ValWithIdx<RecordRef?>?>?> { sourceId: String?, recs: List<ValWithIdx<RecordRef?>?>? ->
            val refs: List<RecordRef?> = recs!!.stream()
                .map(Function<ValWithIdx<RecordRef?>?, RecordRef?> { obj: ValWithIdx<RecordRef?>? -> obj.getValue() })
                .collect(Collectors.toList())
            val atts: List<RecordAtts?>?
            atts = if (!isGatewayMode && !isRemoteSourceId(sourceId)) {
                doWithSchema<List<RecordAtts?>?>(attributes, Function<List<SchemaAtt>, List<RecordAtts?>?> { schema: List<SchemaAtt> -> local!!.getAtts(refs, schema!!, rawAtts) })
            } else if (isGatewayMode || isRemoteRef(recs.stream()
                    .map(Function<ValWithIdx<RecordRef?>?, RecordRef?> { obj: ValWithIdx<RecordRef?>? -> obj.getValue() })
                    .findFirst()
                    .orElse(null))) {
                remote.getAtts(refs, attributes, rawAtts)
            } else {
                doWithSchema<List<RecordAtts?>?>(attributes, Function<List<SchemaAtt>, List<RecordAtts?>?> { schema: List<SchemaAtt> -> local!!.getAtts(refs, schema!!, rawAtts) })
            }
            if (atts == null || atts.size != refs.size) {
                context.addMsg(MsgLevel.ERROR) {
                    "Results count doesn't match with " +
                        "requested. Atts: " + atts + " refs: " + refs
                }
                for (record in recs) {
                    results.add(ValWithIdx<RecordAtts?>(RecordAtts(record.getValue()), record.getIdx()))
                }
            } else {
                for (i in refs.indices) {
                    results.add(ValWithIdx<RecordAtts?>(atts[i], recs[i].getIdx()))
                }
            }
        })
        results.sort(Comparator.comparingInt(ToIntFunction<ValWithIdx<RecordAtts?>?> { obj: ValWithIdx<RecordAtts?>? -> obj.getIdx() }))
        return results.stream().map(Function<ValWithIdx<RecordAtts?>?, RecordAtts?> { obj: ValWithIdx<RecordAtts?>? -> obj.getValue() }).collect(Collectors.toList())
    }

    fun mutate(records: List<RecordAtts?>): List<RecordRef?>? {
        if (remote == null || records.isEmpty()) {
            return local!!.mutate(records)
        }
        return if (isGatewayMode || isRemoteRef(records[0])) {
            remote.mutate(records)
        } else local!!.mutate(records)
    }

    fun delete(records: List<RecordRef?>): List<DelStatus?>? {
        if (remote == null || records.isEmpty()) {
            return local!!.delete(records)
        }
        return if (isGatewayMode || isRemoteRef(records[0])) {
            remote.delete(records)
        } else local!!.delete(records)
    }

    fun getSourceInfo(sourceId: String): RecordsDaoInfo? {
        return if (isGatewayMode || isRemoteSourceId(sourceId)) {
            remote!!.getSourceInfo(sourceId)
        } else local!!.getSourceInfo(sourceId)
    }

    val sourceInfo: List<Any?>
        get() {
            val result: List<RecordsDaoInfo?> = ArrayList<RecordsDaoInfo?>(local!!.getSourceInfo())
            result.addAll(remote!!.getSourceInfo())
            return result
        }

    private fun isRemoteRef(meta: RecordAtts?): Boolean {
        return isRemoteRef(meta.getId())
    }

    private fun isRemoteRef(ref: RecordRef?): Boolean {
        return ref != null && ref.isRemote() && isRemoteSourceId(ref.appName + "/" + ref.sourceId)
    }

    private fun isRemoteSourceId(sourceId: String?): Boolean {
        if (isBlank(sourceId)) {
            return false
        }
        return if (local!!.containsDao(sourceId)) {
            false
        } else sourceId!!.contains("/") && !sourceId.startsWith(currentAppSourceIdPrefix!!)
    }

    fun register(sourceId: String?, recordsDao: RecordsDao?) {
        local!!.register(sourceId, recordsDao)
    }
}
