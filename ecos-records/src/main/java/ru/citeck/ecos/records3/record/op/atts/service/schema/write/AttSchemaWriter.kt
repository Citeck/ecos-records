package ru.citeck.ecos.records3.record.op.atts.service.schema.write

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode
import ecos.com.fasterxml.jackson210.databind.node.NullNode
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.NameUtils
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import java.util.*

interface AttSchemaWriter {

    fun writeToMap(schema: List<SchemaAtt>): Map<String, String> {
        val result: MutableMap<String, String> = LinkedHashMap()
        schema.forEach { att: SchemaAtt -> result[att.alias] = write(att) }
        return result
    }

    fun writeInnerAtts(schema: List<SchemaAtt>, sb: StringBuilder) {
        writeInnerAtts(writeToMap(schema), sb)
    }

    fun writeInnerAtts(attributes: Map<String, String>, sb: StringBuilder) {
        attributes.forEach { (k, v) -> sb.append(k).append(":").append(v) }
    }

    fun unescapeKeys(node: JsonNode?): JsonNode {
        if (node == null) {
            return NullNode.getInstance()
        }
        if (node.size() == 0) {
            return node
        }
        if (node.isObject) {
            val result: ObjectNode = Json.mapper.newObjectNode()
            val names = node.fieldNames()
            while (names.hasNext()) {
                val name = names.next()
                result.set<JsonNode>(NameUtils.unescape(name), unescapeKeys(node.path(name)))
            }
            return result
        }
        if (node.isArray) {
            val array: ArrayNode = Json.mapper.newArrayNode()
            for (innerNode in node) {
                array.add(unescapeKeys(innerNode))
            }
            return array
        }
        return node
    }

    fun write(att: SchemaAtt): String {
        val sb = StringBuilder()
        write(att, sb)
        return sb.toString()
    }

    fun write(attribute: SchemaAtt, out: StringBuilder)
}
