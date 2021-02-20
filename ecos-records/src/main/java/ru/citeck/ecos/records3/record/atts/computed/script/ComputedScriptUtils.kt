package ru.citeck.ecos.records3.record.atts.computed.script

import ru.citeck.ecos.commons.utils.ScriptUtils
import ru.citeck.ecos.records3.utils.AttUtils
import java.lang.RuntimeException

object ComputedScriptUtils {

    fun toRecordAttsMap(attributes: Any?): Pair<Map<String, *>, Boolean>? {

        if (attributes == null) {
            return null
        }
        val atts = ScriptUtils.convertToJava(attributes)
        val attsMap: Map<String, *>
        when (atts) {
            is String -> {
                attsMap = mapOf(Pair(atts, atts))
            }
            is List<*> -> {
                attsMap = AttUtils.toMap(
                    atts.map {
                        it?.toString() ?: ""
                    }.filter {
                        it.isNotBlank()
                    }
                )
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                attsMap = atts as Map<String, *>
            }
            else -> {
                throw RuntimeException("Incorrect attributes object: $attributes of type ${attributes.javaClass}")
            }
        }

        return Pair(attsMap, atts is String)
    }
}
