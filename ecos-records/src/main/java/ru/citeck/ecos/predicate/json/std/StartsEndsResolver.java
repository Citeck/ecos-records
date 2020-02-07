package ru.citeck.ecos.predicate.json.std;

import ecos.com.fasterxml.jackson210.core.JsonProcessingException;
import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.Predicate;

import java.util.Arrays;
import java.util.List;

public class StartsEndsResolver implements PredicateResolver {

    @Override
    public Predicate resolve(ObjectMapper mapper, ObjectNode node) throws JsonProcessingException {

        String type = node.get("t").asText();

        switch (type) {
            case "starts": {
                node = node.deepCopy();
                node.put("t", "like");
                node.put("val", node.get("val").asText() + "%");
                break;
            }
            case "ends": {
                node = node.deepCopy();
                node.put("t", "like");
                node.put("val", "%" + node.get("val").asText());
                break;
            }
            default:
                throw new RuntimeException("Unknown type: " + type);
        }

        return mapper.treeToValue(node, Predicate.class);
    }

    @Override
    public List<String> getTypes() {
        return Arrays.asList("starts", "ends");
    }
}
