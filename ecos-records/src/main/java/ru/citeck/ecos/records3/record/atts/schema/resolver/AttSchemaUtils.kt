package ru.citeck.ecos.records3.record.atts.schema.resolver

import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import java.util.ArrayList

object AttSchemaUtils {

    /**
     * Replace aliases by name and join attributes.
     */
    fun simplifySchema(schema: List<SchemaAtt>): List<SchemaAtt> {
        if (schema.isEmpty()) {
            return schema
        }
        val result = LinkedHashMap<String, SchemaAtt.Builder>()
        for (att in schema) {
            var resAtt = result[att.name]
            if (resAtt == null) {
                resAtt = att.copy()
                    .withAlias(att.name)
                    .withProcessors(emptyList())
                result[att.name] = resAtt
            } else {
                resAtt.withMultiple(resAtt.multiple || att.multiple)
                val innerAtts = ArrayList<SchemaAtt>()
                innerAtts.addAll(resAtt.inner)
                innerAtts.addAll(att.inner)
                resAtt.withInner(innerAtts)
            }
        }
        val resultAtts = ArrayList<SchemaAtt>()
        for (att in result.values) {
            att.withInner(simplifySchema(att.inner))
            resultAtts.add(att.build())
        }
        return resultAtts
    }
}
