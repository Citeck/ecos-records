package ru.citeck.ecos.records2.utils.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.core.JsonParser;
import ecos.com.fasterxml.jackson210.databind.DeserializationContext;
import ecos.com.fasterxml.jackson210.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Bridge between com.fasterxml and ecos.com.fasterxml
 */
public class JsonNodeDeserializer<T extends JsonNode> extends StdDeserializer<JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNodeDeserializer(Class<T> type) {
        super(type);
    }

    @Override
    public JsonNode deserialize(JsonParser jsonParser,
                                DeserializationContext deserializationContext) throws IOException {

        ecos.com.fasterxml.jackson210.databind.JsonNode node = jsonParser.readValueAsTree();
        Object nodeObj = JsonUtils.toJava(node);

        return objectMapper.valueToTree(nodeObj);
    }
}
