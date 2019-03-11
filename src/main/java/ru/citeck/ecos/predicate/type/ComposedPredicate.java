package ru.citeck.ecos.predicate.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class ComposedPredicate implements Predicate {

    private List<Predicate> predicates;

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    @JsonCreator
    public static ComposedPredicate create(ObjectNode node) {


        node.path("val")

    }

    public static class And extends ComposedPredicate {
    }

    public static class Or extends ComposedPredicate {
    }
}
