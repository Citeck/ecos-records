package ru.citeck.ecos.records3.utils

object AttUtils {

    fun toMap(attributes: Collection<String>): Map<String, String> {
        val attributesMap: MutableMap<String, String> = LinkedHashMap()
        for (attribute in attributes) {
            attributesMap[attribute] = attribute
        }
        return attributesMap
    }
}
