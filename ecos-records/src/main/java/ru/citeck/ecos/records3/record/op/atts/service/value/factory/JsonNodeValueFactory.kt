package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.*
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import java.util.ArrayList

class JsonNodeValueFactory : AttValueFactory<JsonNode> {

    override fun getValue(value: JsonNode): AttValue? {

        return object : AttValue {

            override fun asText(): String? {
                return if (value is NullNode || value is MissingNode) {
                    null
                } else if (value.isTextual) {
                    value.asText()
                } else {
                    Json.mapper.toString(value)
                }
            }

            override fun asJson(): Any? {
                return value
            }

            override fun getAtt(name: String): Any? {
                val node = value[name]
                if (node != null && node.isArray) {
                    val result: MutableList<JsonNode?> = ArrayList()
                    for (element in node) {
                        result.add(element)
                    }
                    return result
                }
                return node
            }
        }
    }

    override fun getValueTypes() = listOf(
        ObjectNode::class.java,
        ArrayNode::class.java,
        TextNode::class.java,
        NumericNode::class.java,
        NullNode::class.java,
        MissingNode::class.java,
        BooleanNode::class.java,
        FloatNode::class.java,
        IntNode::class.java,
        DoubleNode::class.java,
        LongNode::class.java
    )
}
