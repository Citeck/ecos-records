package ru.citeck.ecos.predicate.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AndPredicate extends ComposedPredicate {

    private static final String TYPE = "and";

    @JsonProperty("t")
    String getType() {
        return TYPE;
    }

    public static List<String> getTypes() {
        return Collections.singletonList(TYPE);
    }

    public static AndPredicate of(Predicate... predicates) {
        AndPredicate and = new AndPredicate();
        and.setPredicates(Arrays.asList(predicates));
        return and;
    }

    @Override
    public String toString() {
        return "(" + getPredicates().stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(" AND ")) + ")";
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

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
