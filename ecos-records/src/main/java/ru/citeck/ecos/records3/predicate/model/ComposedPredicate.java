package ru.citeck.ecos.records3.predicate.model;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class ComposedPredicate implements Predicate {

    @JsonProperty("val")
    private List<Predicate> predicates;

    public List<Predicate> getPredicates() {
        return predicates == null ? Collections.emptyList() : predicates;
    }

    public void setPredicates(List<Predicate> predicates) {
        if (predicates == null) {
            this.predicates = new ArrayList<>();
        } else {
            this.predicates = new ArrayList<>(predicates);
        }
    }

    public void addPredicate(Predicate predicate) {
        if (predicates == null) {
            predicates = new ArrayList<>();
        }
        predicates.add(predicate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComposedPredicate that = (ComposedPredicate) o;
        return Objects.equals(predicates, that.predicates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicates);
    }
}
