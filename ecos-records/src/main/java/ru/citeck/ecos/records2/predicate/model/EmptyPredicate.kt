package ru.citeck.ecos.records2.predicate.model;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class EmptyPredicate extends AttributePredicate {

    private static final String TYPE = "empty";

    public EmptyPredicate() {
    }

    public EmptyPredicate(String attribute) {
        setAttribute(attribute);
    }

    @JsonProperty("t")
    public String getType() {
        return TYPE;
    }

    public static List<String> getTypes() {
        return Collections.singletonList(TYPE);
    }

    @Override
    public <T extends Predicate> T copy() {

        @SuppressWarnings("unchecked")
        T result = (T) new EmptyPredicate(getAttribute());

        return result;
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
