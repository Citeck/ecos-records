package ru.citeck.ecos.predicate.model;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NotPredicate implements Predicate {

    private static final String TYPE = "not";

    @JsonProperty("val")
    private Predicate predicate;

    public NotPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    public NotPredicate() {
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    @JsonProperty("t")
    String getType() {
        return TYPE;
    }

    public static List<String> getTypes() {
        return Collections.singletonList(TYPE);
    }

    @Override
    public <T extends Predicate> T copy() {

        @SuppressWarnings("unchecked")
        T result = (T) new NotPredicate(predicate.copy());

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
        NotPredicate that = (NotPredicate) o;
        return Objects.equals(predicate, that.predicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate);
    }

    @Override
    public String toString() {
        return "NOT " + getPredicate();
    }
}
