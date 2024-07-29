package ru.citeck.ecos.records2.predicate.json.std;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.predicate.model.ValuePredicate;

import java.util.Arrays;
import java.util.List;

public class StartsEndsResolver implements PredicateResolver {

    @Override
    public Predicate resolve(ObjectCodec mapper, ObjectNode node) throws JsonProcessingException {

        String type = node.get("t").asText();
        String predicateValue = getPredicateValue(node);
        if (predicateValue.isEmpty()) {
            return Predicates.alwaysTrue();
        }

        switch (type) {
            case "starts": {
                node = node.deepCopy();
                node.put("t", ValuePredicate.Type.LIKE.asString());
                node.put("val", predicateValue + "%");
                break;
            }
            case "ends": {
                node = node.deepCopy();
                node.put("t", ValuePredicate.Type.LIKE.asString());
                node.put("val", "%" + predicateValue);
                break;
            }
            default:
                throw new RuntimeException("Unknown type: " + type);
        }

        return mapper.treeToValue(node, Predicate.class);
    }

    private String getPredicateValue(ObjectNode node) {
        JsonNode valNode;
        if (node.has("v")) {
            valNode = node.path("v");
        } else {
            valNode = node.path("val");
        }
        if (valNode.isTextual() || valNode.isNumber() || valNode.isBoolean()) {
            return valNode.asText();
        }
        return "";
    }

    @Override
    public List<String> getTypes() {
        return Arrays.asList("starts", "ends");
    }
}
