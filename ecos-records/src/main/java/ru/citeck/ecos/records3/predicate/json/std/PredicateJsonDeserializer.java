package ru.citeck.ecos.records3.predicate.json.std;

import ecos.com.fasterxml.jackson210.core.JsonParser;
import ecos.com.fasterxml.jackson210.databind.DeserializationContext;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.databind.deser.std.StdDeserializer;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.predicate.model.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard converter for predicates based on jackson library.
 */
@Slf4j
public class PredicateJsonDeserializer extends StdDeserializer<Predicate> {

    private static final long serialVersionUID = 1L;

    private transient Map<String, PredicateResolver> predicateResolvers = new ConcurrentHashMap<>();
    private transient PredicateTypes predicateTypes;

    public PredicateJsonDeserializer(PredicateTypes predicateTypes) {
        super(Predicate.class);
        register(new StartsEndsResolver());
        this.predicateTypes = predicateTypes;
    }

    @Override
    public Predicate deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        ObjectNode predicateNode = mapper.readTree(jp);

        String type = predicateNode.path("t").asText();

        if (StringUtils.isBlank(type)) {
            return VoidPredicate.INSTANCE;
        }

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
        if (inverse) {
            predicate = new NotPredicate(predicate);
        }
        return predicate;
    }

    public void register(PredicateResolver resolver) {
        resolver.getTypes().forEach(t -> predicateResolvers.put(t, resolver));
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }
}
