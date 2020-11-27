package ru.citeck.ecos.records3.record.op.atts.service.computed

import mu.KotlinLogging
import ru.citeck.ecos.commons.utils.ScriptUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.commons.utils.TmplUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.service.computed.script.AttValueScriptCtxImpl
import ru.citeck.ecos.records3.record.op.atts.service.computed.script.RecordsScriptService
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValueCtx

class ComputedAttsService(services: RecordsServiceFactory) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val recordsScriptService by lazy { RecordsScriptService(services) }

    fun compute(context: AttValueCtx, att: ComputedAtt): Any? {

        if (att.def.storingType == StoringType.ON_CREATE && !ComputedUtils.isNewRecord()) {
            return context.getAtt(att.id)
        }
        if (att.def.storingType == StoringType.ON_EMPTY) {
            val currentValue = context.getAtt(att.id)
            if (!currentValue.isNull()) {
                return currentValue.asJavaObj()
            }
        }

        return when (att.def.type) {

            ComputedAttType.SCRIPT -> {

                val script = att.def.config.get("script").asText()

                if (StringUtils.isBlank(script)) {
                    log.warn("Script is blank. Att: $att")
                    return null
                }

                val scriptModel = mapOf(
                    Pair("value", AttValueScriptCtxImpl(context)),
                    Pair("Records", recordsScriptService)
                )

                return ScriptUtils.eval(script, scriptModel)
            }
            ComputedAttType.ATTRIBUTE -> {

                context.getAtt(att.def.config.get("attribute").asText())
            }
            ComputedAttType.VALUE -> {

                val value = att.def.config.get("value")
                if (value.isTextual()) {
                    val text = value.asText()
                    val lowerText = text.toLowerCase()
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
                } else if (value.isNumber()) {
                    value.asDouble()
                } else {
                    value
                }
            }
            ComputedAttType.TEMPLATE -> {

                val value = att.def.config.get("template").asText()
                val atts = context.getAtts(TmplUtils.getAtts(value))

                return TmplUtils.applyAtts(value, atts)
            }
            ComputedAttType.COUNTER, ComputedAttType.NONE -> {

                return context.getAtt(att.id)
            }
        }
    }
}
