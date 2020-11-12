package ru.citeck.ecos.records2.predicate.model;

import ecos.com.fasterxml.jackson210.annotation.JsonValue;

import java.util.Collections;

public class VoidPredicate implements Predicate {

    public static final VoidPredicate INSTANCE = new VoidPredicate();

    private VoidPredicate() {
    }

    @JsonValue
    protected Object jsonValue() {
        return Collections.emptyMap();
    }

    @Override
    public <T extends Predicate> T copy() {
        @SuppressWarnings("unchecked")
        T result = (T) INSTANCE;
        return result;
    }

    @Override
    public String toString() {
        return "()";
    }

    @Override
    public int hashCode() {
        return VoidPredicate.class.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof VoidPredicate;
    }
}
