package ru.citeck.ecos.predicate.parse.std.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.type.Predicate;

import java.util.Set;

public interface StdPredicateType {

    Predicate getPredicate(ObjectMapper mapper, ObjectNode node);

    Set<String> getNames();
}
