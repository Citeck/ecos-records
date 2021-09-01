package ru.citeck.ecos.records3.record.atts.value.factory

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.HasListView
import java.util.ArrayList

class JacksonJsonNodeValueFactory : AttValueFactory<JsonNode> {

    override fun getValue(value: JsonNode): AttValue {

        return object : AttValue, HasListView<JsonNode> {

            override fun getId(): Any? {
                if (value.isTextual) {
                    return value.asText()
                }
                return null
            }

            override fun asText(): String? {
                return if (value is NullNode || value is MissingNode) {
                    null
                } else if (value.isTextual) {
                    value.asText()
                } else {
                    Json.mapper.toString(value)
                }
            }

            override fun getAtt(name: String): Any? {
                val node = value[name]
                if (node != null && node.isArray) {
                    val result: MutableList<JsonNode> = ArrayList()
                    for (element in node) {
                        result.add(element)
                    }
                    return result
                }
                return node
            }

            override fun asJson(): Any {
                return value
            }

            override fun getListView(): List<JsonNode> {
                return if (value.isArray) {
                    value.toList()
                } else {
                    listOf(value)
                }
            }

            override fun getAs(type: String?): Any? {
                if (type == "mltext") {
                    return Json.mapper.convert(value, MLText::class.java)
                }
                return null
            }

            override fun asRaw(): Any {
                return value
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
