package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.*;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.utils.JsonUtils;

import java.util.Arrays;
import java.util.List;

public class JsonNodeValueFactory implements MetaValueFactory<JsonNode> {

    @Override
    public MetaValue getValue(JsonNode value) {

        return new MetaValue() {

            @Override
            public String getString() {
                if (value == null || value instanceof NullNode || value instanceof MissingNode) {
                    return null;
                } else if (value.isTextual()) {
                    return value.asText();
                } else {
                    return JsonUtils.toString(value);
                }
            }

            @Override
            public Object getAttribute(String name, MetaField field) {
                return value.get(name);
            }
        };
    }

    @Override
    public List<Class<? extends JsonNode>> getValueTypes() {
        return Arrays.asList(
                ObjectNode.class,
                ArrayNode.class,
                TextNode.class,
                NumericNode.class,
                NullNode.class,
                MissingNode.class,
                BooleanNode.class,
                FloatNode.class,
                IntNode.class,
                DoubleNode.class,
                LongNode.class
        );
    }
}
