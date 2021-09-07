package ru.citeck.ecos.records3.record.atts.computed

import mu.KotlinLogging
import ru.citeck.ecos.commons.utils.ScriptUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.commons.utils.TmplUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.script.AttValueScriptCtxImpl
import ru.citeck.ecos.records3.record.atts.computed.script.RecordsScriptService
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx

class ComputedAttsService(services: RecordsServiceFactory) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    val recordsScriptService by lazy { RecordsScriptService(services) }

    fun compute(context: AttValueCtx, att: ComputedAtt): Any? {

        if (att.def.storingType == StoringType.ON_CREATE && !ComputedUtils.isNewRecord()) {
            return context.getAtt(att.id)
        }
        if (att.def.storingType == StoringType.ON_EMPTY) {
            val currentValue = context.getAtt(att.id)
            if (!currentValue.isNull() && (!currentValue.isTextual() || currentValue.asText().isNotBlank())) {
                return currentValue.asJavaObj()
            }
        }
        return compute(context, att.def) { context.getAtt(att.id) }
    }

    fun compute(context: AttValueCtx, def: ComputedAttDef): Any? {
        return compute(context, def) { null }
    }

    inline fun compute(context: AttValueCtx, def: ComputedAttDef, orElse: () -> Any?): Any? {

        return when (def.type) {

            ComputedAttType.SCRIPT -> {

                val script = def.config.get("fn").asText()

                if (StringUtils.isBlank(script)) {
                    log.warn("Script is blank. Def: $def")
                    return null
                }

                val scriptModel = mapOf(
                    Pair("value", AttValueScriptCtxImpl(context)),
                    Pair("Records", recordsScriptService)
                )

                ScriptUtils.eval(script, scriptModel)
            }
            ComputedAttType.ATTRIBUTE -> {

                context.getAtt(def.config.get("attribute").asText())
            }
            ComputedAttType.VALUE -> {

                val value = def.config.get("value")
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

                val value = def.config.get("template").asText()
                val atts = context.getAtts(TmplUtils.getAtts(value))

                TmplUtils.applyAtts(value, atts)
            }
            ComputedAttType.COUNTER, ComputedAttType.NONE -> {

                orElse.invoke()
            }
        }
    }
}
