package ru.citeck.ecos.predicate.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrPredicate extends ComposedPredicate {

    public static final String TYPE = "or";

    @JsonProperty("t")
    String getType() {
        return TYPE;
    }

    public static List<String> getTypes() {
        return Collections.singletonList(TYPE);
    }

    @Override
    public String toString() {
        return "(" + getPredicates().stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(" OR ")) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return super.equals(o);
    }
}
