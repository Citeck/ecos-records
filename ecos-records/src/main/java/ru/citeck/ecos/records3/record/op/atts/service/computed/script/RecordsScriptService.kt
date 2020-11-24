package ru.citeck.ecos.records3.record.op.atts.service.computed.script

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.ScriptUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValueCtx
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery

class RecordsScriptService(services: RecordsServiceFactory) {

    private val recordsService = services.recordsServiceV1

    fun get(recordRef: String): AttValueScriptCtx {
        return AttValueScriptCtxImpl(Record(RecordRef.valueOf(recordRef)))
    }

    fun query(query: Any?, attributes: Any?): RecsQueryRes<DataValue> {

        val javaQuery = ScriptUtils.convertToJava(query)
        val recsQuery = Json.mapper.convert(javaQuery, RecordsQuery::class.java) ?: return RecsQueryRes()
        val atts = ComputedScriptUtils.toRecordAttsMap(attributes) ?: return RecsQueryRes()

        val result = recordsService.query(recsQuery, atts.first)
        val flatResult = RecsQueryRes<DataValue>()
        flatResult.setHasMore(result.getHasMore())
        flatResult.setTotalCount(result.getTotalCount())
        flatResult.setRecords(
            result.getRecords().map {
                it.getAtts().getData().copy().set("id", it.getId().toString())
            }
        )

        return flatResult
    }

    private inner class Record(val recordRef: RecordRef) : AttValueCtx {

        override fun getRef(): RecordRef {
            return recordRef
        }

        override fun getLocalId(): String {
            return recordRef.id
        }

        override fun getAtt(attribute: String): DataValue {
            return recordsService.getAtt(recordRef, attribute)
        }

        override fun getAtts(attributes: Map<String, *>): ObjectData {
            return recordsService.getAtts(recordRef, attributes).getAtts()
        }

        override fun <T : Any> getAtts(attributes: Class<T>): T {
            return recordsService.getAtts(recordRef, attributes)
        }
    }
}
