package ru.citeck.ecos.records3.record.atts.value.factory

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.*
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.record.atts.value.AttValue

class JsonNodeValueFactory(
    private val dataValueFactory: DataValueAttFactory
) : AttValueFactory<JsonNode> {

    override fun getValue(value: JsonNode): AttValue {
        return dataValueFactory.getValue(DataValue.create(value))
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
