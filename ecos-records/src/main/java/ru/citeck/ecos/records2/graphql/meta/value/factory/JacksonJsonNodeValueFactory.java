package ru.citeck.ecos.records2.graphql.meta.value.factory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JacksonJsonNodeValueFactory implements MetaValueFactory<JsonNode> {

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
                    return Json.getMapper().toString(value);
                }
            }

            @Override
            public Object getAttribute(String name, MetaField field) {
                JsonNode node = value.get(name);
                if (node != null && node.isArray()) {
                    List<JsonNode> result = new ArrayList<>();
                    for (JsonNode element : node) {
                        result.add(element);
                    }
                    return result;
                }
                return node;
            }

            @Override
            public Object getJson() {
                return value;
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