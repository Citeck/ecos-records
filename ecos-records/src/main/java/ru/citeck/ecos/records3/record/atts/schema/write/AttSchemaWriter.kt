package ru.citeck.ecos.records3.record.atts.schema.write

import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import java.util.*

interface AttSchemaWriter {

    fun writeToMap(schema: List<SchemaAtt>): Map<String, String> {
        val result: MutableMap<String, String> = LinkedHashMap()
        schema.forEach { att: SchemaAtt -> result[att.getAliasForValue()] = write(att) }
        return result
    }

    fun write(att: SchemaAtt): String {
        val sb = StringBuilder()
        write(att, sb, false)
        return sb.toString()
    }

    fun write(attribute: SchemaAtt, out: StringBuilder, firstInBraces: Boolean)
}
