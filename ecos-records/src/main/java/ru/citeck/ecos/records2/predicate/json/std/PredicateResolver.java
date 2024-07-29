package ru.citeck.ecos.records2.predicate.json.std;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.records2.predicate.model.Predicate;

import java.util.List;

public interface PredicateResolver {

    Predicate resolve(ObjectCodec mapper, ObjectNode node) throws JsonProcessingException;

    List<String> getTypes();
}
