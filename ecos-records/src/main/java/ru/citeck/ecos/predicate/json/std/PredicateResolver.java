package ru.citeck.ecos.predicate.json.std;

import ecos.com.fasterxml.jackson210.core.JsonProcessingException;
import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.Predicate;

import java.util.List;

public interface PredicateResolver {

    Predicate resolve(ObjectMapper mapper, ObjectNode node) throws JsonProcessingException;

    List<String> getTypes();
}
