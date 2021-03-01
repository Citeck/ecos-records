package ru.citeck.ecos.records3.utils

import ru.citeck.ecos.records2.RecordConstants

object AttUtils {

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
}
