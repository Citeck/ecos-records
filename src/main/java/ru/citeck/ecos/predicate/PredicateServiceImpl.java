package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.predicate.parse.PredicateParser;
import ru.citeck.ecos.predicate.type.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PredicateServiceImpl implements PredicateService {

    private static final Log logger = LogFactory.getLog(PredicateServiceImpl.class);

    private Map<String, PredicateConverter> converters = new ConcurrentHashMap<>();
    private PredicateParser predicateParser;

    public PredicateServiceImpl() {
    }

    @Override
    public boolean canConvertTo(String language) {
        return converters.containsKey(language);
    }

    @Override
    public JsonNode convert(String language, ObjectNode predicate) {
        PredicateConverter converter = converters.get(language);
        if (converter == null) {
            return null;
        }
        return converter.convert(predicate);
    }

    @Override
    public JsonNode convert(String language, Predicate predicate) {
        return null;
    }

    @Override
    public void register(PredicateConverter converter) {
        converters.put(converter.getLanguage(), converter);
    }

    public void setPredicateParser(PredicateParser predicateParser) {
        this.predicateParser = predicateParser;
    }
}
