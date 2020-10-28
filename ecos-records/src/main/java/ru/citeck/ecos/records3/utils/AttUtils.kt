package ru.citeck.ecos.records3.utils

import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt

object AttUtils {

    fun toMap(attributes: Collection<String>): Map<String, String> {
        val attributesMap: MutableMap<String, String> = LinkedHashMap()
        for (attribute in attributes) {
            attributesMap[attribute] = attribute
        }
        return attributesMap
    }

    fun removeProcessors(att: SchemaAtt): SchemaAtt {
        return if (att.processors.isEmpty() && att.inner.isEmpty()) {
            att
        } else {
            att.copy()
                .withProcessors(emptyList())
                .withInner(att.inner.map { removeProcessors(it) })
                .build()
        }
    }
}
