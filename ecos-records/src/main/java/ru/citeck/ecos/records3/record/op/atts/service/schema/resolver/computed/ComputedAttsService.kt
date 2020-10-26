package ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.computed

import mu.KotlinLogging
import ru.citeck.ecos.commons.utils.ScriptUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.type.ComputedAtt
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttValueCtx
import java.util.*

class ComputedAttsService {

    companion object {
        const val TYPE_SCRIPT = "script"
        const val TYPE_VALUE = "value"
        const val TYPE_ATTRIBUTE = "attribute"

        val log = KotlinLogging.logger {}
    }

    fun compute(context: AttValueCtx, computedAtt: ComputedAtt) : Any? {

        return when (computedAtt.type) {
            TYPE_SCRIPT -> {

                val script = computedAtt.config.get("script").asText()

                if (StringUtils.isBlank(script)) {
                    log.warn("Script is blank. Att: $computedAtt")
                    return null
                }

                return ScriptUtils.eval(script, Collections.singletonMap("value", context))
            }
            TYPE_ATTRIBUTE -> {

                val attribute = computedAtt.config.get("attribute").asText()
                return context.getAtt(attribute)
            }
            TYPE_VALUE -> {

                return computedAtt.config.get("value")
            }
            else -> {
                log.error { "Unknown computed attribute type: '${computedAtt.type}'. Att: $computedAtt" }
                null
            }
        }
    }
}
