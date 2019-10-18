package ru.citeck.ecos.predicate.json.std;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.predicate.json.JsonConverter;
import ru.citeck.ecos.predicate.model.*;
import ru.citeck.ecos.records2.utils.MandatoryParam;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard converter for predicates based on jackson library.
 */
@Slf4j
public class StdJsonConverter extends StdDeserializer<Predicate> implements JsonConverter {

    private Map<String, Class<? extends Predicate>> predicateTypes = new ConcurrentHashMap<>();
    private Map<String, PredicateResolver> predicateResolvers = new ConcurrentHashMap<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    public StdJsonConverter() {
        super(Predicate.class);

        register(OrPredicate.class);
        register(AndPredicate.class);
        register(NotPredicate.class);
        register(ValuePredicate.class);
        register(EmptyPredicate.class);

        register(new StartsEndsResolver());

        SimpleModule module = new SimpleModule("predicates");
        module.addDeserializer(Predicate.class, this);
        objectMapper.registerModule(module);

        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public Predicate deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        ObjectNode predicateNode = mapper.readTree(jp);

        String type = predicateNode.path("t").asText();
        MandatoryParam.checkString("t", type);

        boolean inverse = false;
        if (type.startsWith("not-")) {
            inverse = true;
            type = type.substring(4);
            predicateNode = predicateNode.deepCopy();
            predicateNode.put("t", type);
        }

        Predicate predicate = null;

        Class<? extends Predicate> predicateType = predicateTypes.get(type);

        if (predicateType != null) {

            if (AndPredicate.class.equals(predicateType)
                    || OrPredicate.class.equals(predicateType)) {

                JsonNode children = predicateNode.get("val");
                if (children instanceof ArrayNode && children.size() == 1) {
                    predicate = mapper.treeToValue(children.get(0), Predicate.class);
                }
            }
            if (predicate == null) {
                predicate = mapper.treeToValue(predicateNode, predicateType);
            }

        } else {

            PredicateResolver resolver = predicateResolvers.get(type);
            if (resolver != null) {
                predicate = resolver.resolve(mapper, predicateNode);
            }
        }

        if (predicate == null) {
            throw ctxt.mappingException("Type is unknown: " + type);
        }

        return inverse ? new NotPredicate(predicate) : predicate;
    }

    @Override
    public Predicate fromJson(ObjectNode node) {
        try {
            return objectMapper.treeToValue(node, Predicate.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ObjectNode toJson(Predicate predicate) {
        return objectMapper.valueToTree(predicate);
    }

    public void register(PredicateResolver resolver) {
        resolver.getTypes().forEach(t -> predicateResolvers.put(t, resolver));
    }

    public void register(Class<? extends Predicate> type) {

        Method getTypes = null;

        try {
            getTypes = type.getMethod("getTypes");
        } catch (NoSuchMethodException e) {
            log.error("Method getTypes not found in " + type, e);
        }

        if (getTypes == null) {
            throw new RuntimeException("Predicate type should have static method getTypes");
        }

        try {

            @SuppressWarnings("unchecked")
            Collection<String> types = (Collection<String>) getTypes.invoke(null);

            types.forEach(n ->
                    predicateTypes.put(n, type)
            );

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
