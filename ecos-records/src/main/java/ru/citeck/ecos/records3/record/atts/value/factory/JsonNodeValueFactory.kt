package ru.citeck.ecos.records3.record.atts.value.factory

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.*
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter

class JsonNodeValueFactory : AttValueFactory<JsonNode> {

    private lateinit var dataValueFactory: DataValueAttFactory

    override fun init(attValuesConverter: AttValuesConverter) {
        this.dataValueFactory = attValuesConverter.getFactory(DataValueAttFactory::class.java)
    }

    override fun getValue(value: JsonNode): AttValue? {
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
