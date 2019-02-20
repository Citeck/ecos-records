package ru.citeck.ecos.records2.graphql.meta.value.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.Arrays;
import java.util.List;

public class JsonNodeValueFactory implements MetaValueFactory<JsonNode> {

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public MetaValue getValue(JsonNode value) {

        return new MetaValue() {

            @Override
            public String getString() {
                try {
                    if (value == null || value instanceof NullNode || value instanceof MissingNode) {
                        return null;
                    } else if (value.isTextual()) {
                        return value.asText();
                    } else {
                        return mapper.writeValueAsString(value);
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Error! value: " + value);
                }
            }

            @Override
            public Object getAttribute(String name) {
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
