package ru.citeck.ecos.predicate.json.std;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.citeck.ecos.predicate.model.Predicate;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        }

        return mapper.treeToValue(node, Predicate.class);
    }

    @Override
    public List<String> getTypes() {
        return Arrays.asList("starts", "ends");
    }
}
