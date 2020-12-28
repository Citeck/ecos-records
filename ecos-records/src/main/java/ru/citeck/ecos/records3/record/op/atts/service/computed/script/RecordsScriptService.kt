package ru.citeck.ecos.records3.record.op.atts.service.computed.script

import jdk.nashorn.internal.objects.NativeArray
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.ScriptUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValueCtx
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery

class RecordsScriptService(services: RecordsServiceFactory) {

    private val recordsService = services.recordsServiceV1

    fun get(record: Any): AttValueScriptCtx {
        if (record is AttValueScriptCtx) {
            return record
        }
        val recordRef = when (record) {
            is RecordRef -> record
            is String -> RecordRef.valueOf(record)
            else -> error("Incorrect record: $record")
        }
        return AttValueScriptCtxImpl(Record(recordRef))
    }

    private fun getEmptyRes(): Any {
        return linkedMapOf(
            Pair("hasMore", false),
            Pair("totalCount", 0),
            Pair("records", NativeArray.construct(true, null)),
            Pair("messages", NativeArray.construct(true, null))
        )
    }

    fun query(query: Any?, attributes: Any?): Any {

        val javaQuery = ScriptUtils.convertToJava(query)
        val recsQuery = Json.mapper.convert(javaQuery, RecordsQuery::class.java) ?: return getEmptyRes()
        val atts = ComputedScriptUtils.toRecordAttsMap(attributes) ?: return getEmptyRes()

        val result = recordsService.query(recsQuery, atts.first)
        val flatResult = LinkedHashMap<String, Any?>()

        flatResult["hasMore"] = result.getHasMore()
        flatResult["totalCount"] = result.getTotalCount()
        flatResult["records"] = result.getRecords().map {
            it.getAtts().getData().copy().set("id", it.getId().toString())
        }
        flatResult["messages"] = NativeArray.construct(true, null)

        return ScriptUtils.convertToScript(flatResult) ?: error("Conversion error. Result: $flatResult")
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

        override fun getAtts(attributes: Collection<String>): ObjectData {
            return recordsService.getAtts(recordRef, attributes).getAtts()
        }

        override fun <T : Any> getAtts(attributes: Class<T>): T {
            return recordsService.getAtts(recordRef, attributes)
        }
    }
}
