package ru.citeck.ecos.records2.predicate.json.std;

import ecos.com.fasterxml.jackson210.core.JsonProcessingException;
import ecos.com.fasterxml.jackson210.core.ObjectCodec;
import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ru.citeck.ecos.records2.predicate.model.Predicate;

import java.util.List;

public interface PredicateResolver {

    Predicate resolve(ObjectCodec mapper, ObjectNode node) throws JsonProcessingException;

    List<String> getTypes();
}
