package ru.citeck.ecos.records3.predicate.json.std;

import ecos.com.fasterxml.jackson210.core.JsonProcessingException;
import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ru.citeck.ecos.records3.predicate.model.Predicate;

import java.util.List;

public interface PredicateResolver {

    Predicate resolve(ObjectMapper mapper, ObjectNode node) throws JsonProcessingException;

    List<String> getTypes();
}
