package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.json.JsonConverter;
import ru.citeck.ecos.predicate.json.std.StdJsonConverter;

import java.io.IOException;

public class PredicateServiceImpl implements PredicateService {

    private static final Log logger = LogFactory.getLog(PredicateServiceImpl.class);

    private JsonConverter jsonConverter;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Predicate readJson(ObjectNode predicateNode) {
        return getJsonConverter().fromJson(predicateNode);
    }

    @Override
    public Predicate readJson(String predicateJson) {
        try {
            return getJsonConverter().fromJson((ObjectNode) objectMapper.readTree(predicateJson));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
