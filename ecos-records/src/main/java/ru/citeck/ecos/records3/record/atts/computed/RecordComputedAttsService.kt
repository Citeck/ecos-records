package ru.citeck.ecos.records3.record.atts.computed

import mu.KotlinLogging
import ru.citeck.ecos.commons.utils.ScriptUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.commons.utils.TmplUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.script.AttValueScriptCtxImpl
import ru.citeck.ecos.records3.record.atts.computed.script.RecordsScriptService
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.atts.value.RecordAttValueCtx

class RecordComputedAttsService(services: RecordsServiceFactory) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val recordsScriptService by lazy { RecordsScriptService(services) }
    private val recordsService by lazy { services.recordsServiceV1 }

    fun compute(value: Any, att: RecordComputedAtt, orElse: () -> Any? = { null }): Any? {
        val valueCtx = if (value is AttValueCtx) {
            value
        } else {
            RecordAttValueCtx(value, recordsService)
        }
        return compute(valueCtx, RecordComputedAttValue(att.type, att.config), orElse)
    }

    fun compute(context: AttValueCtx, att: RecordComputedAtt, orElse: () -> Any? = { null }): Any? {
        return compute(context, RecordComputedAttValue(att.type, att.config), orElse)
    }

    fun compute(context: AttValueCtx, att: RecordComputedAttValue, orElse: () -> Any? = { null }): Any? {

        return when (att.type) {

            RecordComputedAttType.SCRIPT -> {

                val script = att.config.get("fn").asText()

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

                context.getAtt(att.config.get("attribute").asText())
            }
            RecordComputedAttType.VALUE -> {

                val value = att.config.get("value")
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

                val value = att.config.get("template").asText()
                val atts = context.getAtts(TmplUtils.getAtts(value))

                TmplUtils.applyAtts(value, atts)
            }
            RecordComputedAttType.NONE -> {

                orElse.invoke()
            }
        }
    }
}
