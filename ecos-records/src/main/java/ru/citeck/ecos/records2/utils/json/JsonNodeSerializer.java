package ru.citeck.ecos.records2.utils.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.core.JsonGenerator;
import ecos.com.fasterxml.jackson210.databind.SerializerProvider;
import ecos.com.fasterxml.jackson210.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Bridge between com.fasterxml and ecos.com.fasterxml
 */
public class JsonNodeSerializer extends StdSerializer<JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNodeSerializer() {
        super(JsonNode.class);
    }

    @Override
    public void serialize(JsonNode node,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {

        Object obj = objectMapper.treeToValue(node, Object.class);
        jsonGenerator.writeObject(obj);
    }
}
