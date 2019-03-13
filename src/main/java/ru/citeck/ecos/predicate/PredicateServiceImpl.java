package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.json.JsonConverter;
import ru.citeck.ecos.predicate.json.std.StdJsonConverter;

import java.io.IOException;

public class PredicateServiceImpl implements PredicateService {

    private JsonConverter jsonConverter;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Predicate readJson(JsonNode predicateNode) {

        if (predicateNode == null) {
            return null;
        }

        JsonNode objNode = predicateNode;

        if (objNode.isTextual()) {
            try {
                objNode = objectMapper.readTree(objNode.asText());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (objNode instanceof ObjectNode) {
            return getJsonConverter().fromJson((ObjectNode) objNode);
        }

        return null;
    }

    @Override
    public Predicate readJson(String predicateJson) {
        return readJson(TextNode.valueOf(predicateJson));
    }

    @Override
    public ObjectNode writeJson(Predicate predicate) {
        return getJsonConverter().toJson(predicate);
    }

    private JsonConverter getJsonConverter() {
        JsonConverter converter = this.jsonConverter;
        if (converter == null) {
            synchronized (this) {
                converter = this.jsonConverter;
                if (converter == null) {
                    converter = new StdJsonConverter();
                    this.jsonConverter = converter;
                }
            }
        }
        return converter;
    }

    public void setJsonConverter(JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
    }
}
