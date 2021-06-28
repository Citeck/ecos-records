package ru.citeck.ecos.records3.record.resolver

import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
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
import java.util.*

class LocalRemoteResolver(private val services: RecordsServiceFactory) {

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

        if (remote == null) {
            return doWithSchema(attributes) { atts -> local.getAtts(records, atts, rawAtts) }
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

            val refs: List<RecordRef> = recs.map { it.value }

            val atts: List<RecordAtts> = if (!isGatewayMode && !isRemoteSourceId(sourceId)) {
                doWithSchema(attributes) { schema -> local.getAtts(refs, schema, rawAtts) }
            } else if (isGatewayMode || isRemoteRef(recs.map { it.value }.firstOrNull())) {
                remote.getAtts(refs, attributes, rawAtts)
            } else {
                doWithSchema(attributes) { schema -> local.getAtts(refs, schema, rawAtts) }
            }
            if (atts.size != refs.size) {
                context.addMsg(MsgLevel.ERROR) {
                    "Results count doesn't match with " +
                        "requested. Atts: " + atts + " refs: " + refs
                }
                for (record in recs) {
                    results.add(ValWithIdx(RecordAtts(record.value), record.idx))
                }
            } else {
                for (i in refs.indices) {
                    results.add(ValWithIdx(atts[i], recs[i].idx))
                }
            }
        }
        results.sortBy { it.idx }
        return results.map { it.value }
    }

    fun mutate(records: List<RecordAtts>): List<RecordRef> {
        if (remote == null || records.isEmpty()) {
            return local.mutate(records)
        }
        return if (isGatewayMode || isRemoteRef(records[0])) {
            remote.mutate(records)
        } else {
            local.mutate(records)
        }
    }

    fun delete(records: List<RecordRef>): List<DelStatus> {
        if (remote == null || records.isEmpty()) {
            return local.delete(records)
        }
        return if (isGatewayMode || isRemoteRef(records[0])) {
            remote.delete(records)
        } else {
            local.delete(records)
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
}
