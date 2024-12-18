package ru.citeck.ecos.records3.record.atts.computed.script

import org.graalvm.polyglot.HostAccess.Export
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.script.ScriptUtils
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef

class RecordsScriptService(services: RecordsServiceFactory) {

    private val recordsService = services.recordsService

    private lateinit var implCreator: ValueScriptContextCreator

    init {
        implCreator = { ref ->
            AttValueScriptCtxImpl(ScriptRecord(ref), recordsService, implCreator)
        }
    }

    @Export
    fun get(record: Any): AttValueScriptCtx {
        if (record is AttValueScriptCtx) {
            return record
        }
        val recordRef = when (record) {
            is EntityRef -> record
            is String -> EntityRef.valueOf(record)
            else -> error("Incorrect record: $record")
        }
        return AttValueScriptCtxImpl(ScriptRecord(recordRef), recordsService, implCreator)
    }

    private fun getEmptyQueryRes(): Any {
        return linkedMapOf(
            Pair("hasMore", false),
            Pair("totalCount", 0),
            Pair("records", ArrayList<Any?>(0)),
            Pair("messages", ArrayList<Any?>(0))
        )
    }

    @Export
    fun queryOne(query: Any?): String? {
        val recsQuery = convertRecsQuery(query) ?: return null
        return recordsService.queryOne(recsQuery)?.toString()
    }

    @Export
    fun queryOne(query: Any?, attributes: Any?): Any? {

        val atts = ComputedScriptUtils.toRecordAttsMap(attributes)
            ?: return queryOne(query)

        val recsQuery = convertRecsQuery(query) ?: return null
        val resultAtts = recordsService.queryOne(recsQuery, atts.first) ?: return null
        if (atts.second) {
            val resAtts = resultAtts.getAtts()
            if (resAtts.isEmpty()) {
                return null
            }
            val value = resAtts[resAtts.fieldNamesList().first()]
            return ScriptUtils.convertToScript(value)
        }
        val resAtts = resultAtts.getAtts().getData()
        if (!resAtts.has("id")) {
            resAtts["id"] = resultAtts.getId().toString()
        }
        return ScriptUtils.convertToScript(resAtts)
    }

    @Export
    fun query(query: Any?): Any {

        val recsQuery = convertRecsQuery(query) ?: return getEmptyQueryRes()

        val result = recordsService.query(recsQuery)
        val flatResult = LinkedHashMap<String, Any?>()

        flatResult["hasMore"] = result.getHasMore()
        flatResult["totalCount"] = result.getTotalCount()
        flatResult["records"] = result.getRecords().map { it.toString() }
        flatResult["messages"] = ArrayList<Any?>(0)

        return ScriptUtils.convertToScript(flatResult) ?: error("Conversion error. Result: $flatResult")
    }

    @Export
    fun query(query: Any?, attributes: Any?): Any {

        val atts = ComputedScriptUtils.toRecordAttsMap(attributes)
            ?: return query(query)

        val recsQuery = convertRecsQuery(query) ?: return getEmptyQueryRes()

        val result = recordsService.query(recsQuery, atts.first)
        val flatResult = LinkedHashMap<String, Any?>()

        flatResult["hasMore"] = result.getHasMore()
        flatResult["totalCount"] = result.getTotalCount()
        flatResult["records"] = result.getRecords().map {
            val data = it.getAtts().getData()
            if (!data.has("id")) {
                data["id"] = it.getId().toString()
            }
            data
        }
        flatResult["messages"] = ArrayList<Any?>(0)

        return ScriptUtils.convertToScript(flatResult) ?: error("Conversion error. Result: $flatResult")
    }

    private fun convertRecsQuery(query: Any?): RecordsQuery? {
        val javaQuery = ScriptUtils.convertToJava(query)
        return Json.mapper.convert(javaQuery, RecordsQuery::class.java)
    }

    inner class ScriptRecord(val recordRef: EntityRef) : AttValueCtx {

        private val typeRefValue by lazy {
            recordsService.getAtt(recordRef, RecordConstants.ATT_TYPE + "?id")
                .asText()
                .toEntityRef()
        }

        @Export
        override fun getValue(): Any {
            return recordRef
        }

        @Export
        override fun getRef(): EntityRef {
            return recordRef
        }

        @Export
        override fun getTypeRef(): EntityRef {
            return typeRefValue
        }

        @Export
        override fun getTypeId(): String {
            return typeRefValue.getLocalId()
        }

        @Export
        override fun getRawRef(): EntityRef {
            return getRef()
        }

        @Export
        override fun getLocalId(): String {
            return recordRef.getLocalId()
        }

        @Export
        override fun getAtt(attribute: String): DataValue {
            return recordsService.getAtt(recordRef, attribute)
        }

        @Export
        override fun getAtts(attributes: Map<String, *>): ObjectData {
            return recordsService.getAtts(recordRef, attributes).getAtts()
        }

        @Export
        override fun getAtts(attributes: Collection<String>): ObjectData {
            return recordsService.getAtts(recordRef, attributes).getAtts()
        }

        @Export
        override fun <T : Any> getAtts(attributes: Class<T>): T {
            return recordsService.getAtts(recordRef, attributes)
        }
    }
}
