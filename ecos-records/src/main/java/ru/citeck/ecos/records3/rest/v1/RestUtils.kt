package ru.citeck.ecos.records3.rest.v1

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json

object RestUtils {

    fun prepareReqAtts(attributes: Any?): DataValue {
        val node: JsonNode = Json.mapper.toJson(attributes)
        return if (node.isArray) {
            val objNode: ObjectNode = Json.mapper.newObjectNode()
            node.forEach { n -> objNode.set<ObjectNode>(n.asText(), n) }
            DataValue.create(objNode)
        } else {
            DataValue.create(node)
        }
    }

    fun prepareReqAttsAsMap(attributes: Any?): Map<String, Any> {
        attributes ?: return emptyMap()
        val result = hashMapOf<String, Any>()
        val atts = prepareReqAtts(attributes)
        if (atts.isNull() || atts.size() == 0) {
            return emptyMap()
        }
        if (atts.isObject()) {
            atts.forEach { k, v ->
                val value: Any = if (v.isTextual()) {
                    v.asText()
                } else {
                    v
                }
                result[k] = value
            }
        } else {
            error("Attributes format error: ")
        }
        return prepareReqAtts(attributes).asMap(String::class.java, Any::class.java)
    }
}
