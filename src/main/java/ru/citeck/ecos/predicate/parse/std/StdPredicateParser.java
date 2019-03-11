package ru.citeck.ecos.predicate.parse.std;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.predicate.parse.PredicateParser;
import ru.citeck.ecos.predicate.parse.std.type.StdPredicateType;
import ru.citeck.ecos.predicate.type.*;
import ru.citeck.ecos.records2.utils.MandatoryParam;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StdPredicateParser extends StdDeserializer<Predicate> implements PredicateParser {

    private static final Log logger = LogFactory.getLog(StdPredicateParser.class);

    private Map<String, StdPredicateType> predicateTypes = new ConcurrentHashMap<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    public StdPredicateParser() {
        super(Predicate.class);

     /*   predicateTypes.put("and", CompPredicate.And.class);
        predicateTypes.put("or", CompPredicate.Or.class);

        predicateTypes.put("range", AttRangePredicate.class);
        predicateTypes.put("not", NotPredicate.class);

        predicateTypes.put("eq", AttValuePredicate.class);
        predicateTypes.put("gt", AttValuePredicate.class);
        predicateTypes.put("ge", AttValuePredicate.class);
        predicateTypes.put("lt", AttValuePredicate.class);
        predicateTypes.put("le", AttValuePredicate.class);
        predicateTypes.put("in", AttValuePredicate.class);
        predicateTypes.put("like", AttValuePredicate.class);

        predicateTypes.put("empty", AttPredicate.Empty.class);
        predicateTypes.put("null", AttPredicate.Null.class);*/
    }

    /* @JsonCreator
    public static Predicate create(ObjectNode node) {

        String type = node.path("t").asText();

        AttPredicate result = null;

        switch (type) {
            case "null":
                result = new Null();
                break;
            case "empty":
                result = new Empty();
                break;
        }

        if (result == null) {
            throw new IllegalStateException("Unknown type: " + type);
        }

        result.init(node);
        return result;
    }*/

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
        }

        if ("starts".equals(type)) {

            type = "like";
            predicateNode = predicateNode.deepCopy();
            predicateNode.put("val", predicateNode.path("val").asText() + "%");

        } else if ("ends".equals(type)) {

            type = "like";
            predicateNode = predicateNode.deepCopy();
            predicateNode.put("val", "%" + predicateNode.path("val").asText());
        }

        if (type.equals("and")) {

        }

        Class<? extends Predicate> predicateType = predicateTypes.get(type);

        if (predicateType == null) {
            throw ctxt.mappingException("Type is unknown: " + type);
        }

        Predicate predicate = mapper.treeToValue(predicateNode, predicateType);
        return inverse ? new NotPredicate(predicate) : predicate;
    }

    @Override
    public Predicate parse(ObjectNode node) {
        try {
            return objectMapper.treeToValue(node, Predicate.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void register(StdPredicateType type) {
        type.getNames().forEach(n -> {
            if (predicateTypes.putIfAbsent(n, type) != null) {
                logger.warn("Predicate with name " + n + " already registered. Skip: " + type);
            }
        });
    }
}
