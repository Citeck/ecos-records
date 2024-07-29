package ru.citeck.ecos.records3.rest.v1

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
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

    fun prepareReqAttsAsListOfMap(attributes: Any?): List<Map<String, Any>> {
        attributes ?: return emptyList()
        var data = DataValue.create(attributes)
        if (data.isArray()) {
            if (data.isEmpty()) {
                return emptyList()
            }
            val firstElement = data[0]
            if (firstElement.isTextual()) {
                val resultMap = LinkedHashMap<String, Any>()
                for (element in data) {
                    resultMap[element.asText()] = element.asText()
                }
                return listOf(resultMap)
            }
        } else {
            data = DataValue.createArr().add(data)
        }
        if (data.isEmpty()) {
            return emptyList()
        }
        val result = ArrayList<Map<String, Any>>()
        for (atts in data) {
            if (!atts.isObject()) {
                error("Attributes format error: $attributes")
            }
            val elementAtts = LinkedHashMap<String, Any>()
            atts.forEach { k, v ->
                elementAtts[k] = if (v.isTextual()) {
                    v.asText()
                } else {
                    v
                }
            }
            result.add(elementAtts)
        }
        return result
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
