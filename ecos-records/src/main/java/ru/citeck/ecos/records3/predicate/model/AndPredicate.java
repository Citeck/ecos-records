package ru.citeck.ecos.records3.predicate.model;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;

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
    public <T extends Predicate> T copy() {

        AndPredicate copy = new AndPredicate();
        copy.setPredicates(getPredicates().stream()
                                          .map(p -> (Predicate) p.copy())
                                          .collect(Collectors.toList()));
        @SuppressWarnings("unchecked")
        T result = (T) copy;

        return result;
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
