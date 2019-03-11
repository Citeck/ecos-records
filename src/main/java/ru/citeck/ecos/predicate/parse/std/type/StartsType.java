package ru.citeck.ecos.predicate.parse.std.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.type.Predicate;

import java.util.Collections;
import java.util.Set;

public class StartsType implements StdPredicateType {

    @Override
    public Predicate getPredicate(ObjectMapper mapper, ObjectNode node) {



        return null;
    }

    @Override
    public Set<String> getNames() {
        return Collections.singleton("starts");
    }
}
