package ru.citeck.ecos.records3.record.atts.computed

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.ScriptUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.commons.utils.TmplUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.script.AttValueScriptCtxImpl
import ru.citeck.ecos.records3.record.atts.computed.script.RecordsScriptService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.atts.value.RecordAttValueCtx
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordComputedAttsService(services: RecordsServiceFactory) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val recordsScriptService by lazy { RecordsScriptService(services) }
    private val recordsService by lazy { services.recordsServiceV1 }
    private val authoritiesApi by lazy { services.getEcosWebAppApi()?.getAuthoritiesApi() }

    fun compute(value: Any, att: RecordComputedAtt, orElse: () -> Any? = { null }): Any? {
        val valueCtx = if (value is AttValueCtx) {
            value
        } else {
            RecordAttValueCtx(value, recordsService)
        }
        return compute(valueCtx, RecordComputedAttValue(att.type, att.config, att.resultType), orElse)
    }

    fun compute(context: AttValueCtx, att: RecordComputedAtt, orElse: () -> Any? = { null }): Any? {
        return compute(context, RecordComputedAttValue(att.type, att.config, att.resultType), orElse)
    }

    private fun addDefaultScalarForAtt(attribute: String): String {
        if (attribute.contains("{") || attribute.contains('?')) {
            return attribute
        }
        return attribute + ScalarType.RAW.schema
    }

    fun compute(context: AttValueCtx, att: RecordComputedAttValue, orElse: () -> Any? = { null }): Any? {

        val resultValue = when (att.type) {

            RecordComputedAttType.SCRIPT -> {

                val script = att.config["fn"].asText()

                if (StringUtils.isBlank(script)) {
                    log.warn("Script is blank. Def: $att")
                    return null
                }

                val scriptModel = mapOf(
                    Pair("value", AttValueScriptCtxImpl(context)),
                    Pair("Records", recordsScriptService)
                )

                ScriptUtils.eval(script, scriptModel)
            }
            RecordComputedAttType.ATTRIBUTE -> {

                val attribute = att.config["attribute"].asText()
                context.getAtt(addDefaultScalarForAtt(attribute))
            }
            RecordComputedAttType.VALUE -> {

                val value = att.config["value"]
                if (value.isTextual()) {
                    val text = value.asText()
                    val lowerText = text.lowercase()
                    if (lowerText == "true" || lowerText == "false") {
                        value.asBoolean()
                    } else {
                        val numRes = value.asDouble(Double.NaN)
                        if (!numRes.isNaN()) {
                            numRes
                        } else {
                            text
                        }
                    }
                } else if (value.isFloatingPointNumber()) {
                    value.asDouble()
                } else if (value.isIntegralNumber()) {
                    value.asLong()
                } else if (value.isBoolean()) {
                    value.asBoolean()
                } else {
                    value
                }
            }
            RecordComputedAttType.TEMPLATE -> {

                val value = att.config["template"].asText()
                val attsToLoad = TmplUtils.getAtts(value).associateWith { addDefaultScalarForAtt(it) }
                val atts = context.getAtts(attsToLoad)

                TmplUtils.applyAtts(value, atts)
            }
            RecordComputedAttType.NONE -> {

                orElse.invoke()
            }
        }

        if (att.resultType == RecordComputedAttResType.ANY) {
            return resultValue
        }
        if (resultValue == null || resultValue is DataValue && resultValue.isNull()) {
            return null
        }
        var resType = att.resultType
        if (resType == RecordComputedAttResType.AUTHORITY && authoritiesApi == null) {
            resType = RecordComputedAttResType.REF
        }
        return when (resType) {
            RecordComputedAttResType.AUTHORITY -> {
                convertResult(resultValue) {
                    var value: Any? = it
                    if (it is DataValue) {
                        value = it.asJavaObj()
                    }
                    val result = authoritiesApi!!.getAuthorityRef(value)
                    if (result.isEmpty()) {
                        null
                    } else {
                        result
                    }
                }
            }
            RecordComputedAttResType.REF -> {
                convertResult(resultValue) { EntityRef.valueOf(it) }
            }
            RecordComputedAttResType.MLTEXT -> {
                convertResult(resultValue) {
                    when (it) {
                        is String -> MLText(it)
                        is DataValue -> when {
                            it.isObject() -> Json.mapper.convert(it, MLText::class.java)
                            else -> MLText(it.asText())
                        }
                        is Map<*, *> -> Json.mapper.convert(it, MLText::class.java)
                        is ObjectData -> Json.mapper.convert(it.getData(), MLText::class.java)
                        else -> MLText(resType.toString())
                    }
                }
            }
            RecordComputedAttResType.TEXT -> {
                convertResult(resultValue) {
                    when (it) {
                        is String -> it
                        is DataValue -> it.asText()
                        else -> resType.toString()
                    }
                }
            }
            else -> resultValue
        }
    }

    private fun convertResult(value: Any?, action: (Any) -> Any?): Any? {
        if (value == null) {
            return null
        }
        return if (value is Collection<*>) {
            value.mapNotNull { convertResult(it, action) }
        } else {
            action.invoke(value)
        }
    }
}
