package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.Predicate;

public interface PredicateService {

    String LANGUAGE = "predicate";

    Predicate readJson(ObjectNode predicate);

    Predicate readJson(String predicate);

    ObjectNode writeJson(Predicate predicate);
}
