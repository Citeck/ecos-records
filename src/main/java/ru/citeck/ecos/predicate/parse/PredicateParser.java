package ru.citeck.ecos.predicate.parse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.type.Predicate;

public interface PredicateParser {

    Predicate parse(ObjectNode node);
}
