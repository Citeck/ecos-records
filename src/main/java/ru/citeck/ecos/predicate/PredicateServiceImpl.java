package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.predicate.conv.PredicateConverter;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.json.JsonConverter;
import ru.citeck.ecos.predicate.json.std.StdJsonConverter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PredicateServiceImpl implements PredicateService {

    private static final Log logger = LogFactory.getLog(PredicateServiceImpl.class);

    private Map<String, PredicateConverter> converters = new ConcurrentHashMap<>();
    private JsonConverter jsonConverter;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean canConvertTo(String language) {
        return converters.containsKey(language);
    }

    @Override
    public JsonNode toLanguage(String language, ObjectNode predicate) {
        return toLanguage(language, fromJson(predicate));
    }

    @Override
    public JsonNode toLanguage(String language, Predicate predicate) {
        PredicateConverter converter = converters.get(language);
        if (converter == null) {
            return null;
        }
        return converter.convert(predicate);
    }

    @Override
    public void register(PredicateConverter converter) {
        converters.put(converter.getLanguage(), converter);
    }

    @Override
    public Predicate fromJson(ObjectNode predicateNode) {
        return getJsonConverter().fromJson(predicateNode);
    }

    @Override
    public Predicate fromJson(String predicateJson) {
        try {
            return getJsonConverter().fromJson((ObjectNode) objectMapper.readTree(predicateJson));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ObjectNode toJson(Predicate predicate) {
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
