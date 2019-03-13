package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.Predicate;

public interface PredicateService {

    Predicate readJson(JsonNode predicate);

    Predicate readJson(String predicate);

    ObjectNode writeJson(Predicate predicate);
}
