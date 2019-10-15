package ru.citeck.ecos.predicate.json.std;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.Predicate;

import java.util.List;

public interface PredicateResolver {

    Predicate resolve(ObjectMapper mapper, ObjectNode node) throws JsonProcessingException;

    List<String> getTypes();
}
