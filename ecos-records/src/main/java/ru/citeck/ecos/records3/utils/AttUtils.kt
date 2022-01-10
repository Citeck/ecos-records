package ru.citeck.ecos.records3.utils

import org.slf4j.LoggerFactory
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttValue
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.AttValue

object AttUtils {

    private var log = LoggerFactory.getLogger(AttUtils::class.java)

    fun toMap(attributes: Collection<String>): Map<String, String> {
        val attributesMap: MutableMap<String, String> = LinkedHashMap()
        for (attribute in attributes) {
            attributesMap[attribute] = attribute
        }
        return attributesMap
    }

    fun isValidComputedAtt(att: String?): Boolean {
        if (att.isNullOrBlank()) {
            return false
        }
        return att != RecordConstants.ATT_TYPE &&
            att.length > 1 &&
            !att.contains('?') &&
            att[0] != '.' &&
            att[0] != ' '
    }

    fun logError(value: AttValue, msg: String) {
        var id: Any? = null
        try {
            id = value.id
        } catch (ignore: Exception) {
            // do nothing
        }
        log.error("Attribute error. Value id: '$id' path: '${AttContext.getCurrentAttPath()}' ($msg)")
    }

    fun isGlobalContextAtt(value: SchemaAtt, contextAtts: Map<String, *>): Boolean {
        val name = value.name
        if (!name.startsWith('$')) {
            return false
        }
        val ctxAttValue = contextAtts[name.substring(1)]
        return ctxAttValue != null && ctxAttValue !is RecordComputedAttValue
    }
}
