package ru.citeck.ecos.predicate.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class EmptyPredicate extends AttributePredicate {

    private static final String TYPE = "empty";

    @JsonProperty("t")
    public String getType() {
        return TYPE;
    }

    public static List<String> getTypes() {
        return Collections.singletonList(TYPE);
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
