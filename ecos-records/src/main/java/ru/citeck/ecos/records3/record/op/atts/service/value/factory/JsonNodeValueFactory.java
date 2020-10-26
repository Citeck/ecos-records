package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.*;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonNodeValueFactory implements AttValueFactory<JsonNode> {

    @Override
    public AttValue getValue(JsonNode value) {

        return new AttValue() {

            @Override
            public String asText() {
                if (value == null || value instanceof NullNode || value instanceof MissingNode) {
                    return null;
                } else if (value.isTextual()) {
                    return value.asText();
                } else {
                    return Json.getMapper().toString(value);
                }
            }

            @Override
            public Object asJson() {
                return value;
            }

            @Override
            public Object getAtt(@NotNull String name) {
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
