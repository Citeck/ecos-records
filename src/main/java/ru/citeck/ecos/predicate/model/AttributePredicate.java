package ru.citeck.ecos.predicate.model;

import ru.citeck.ecos.predicate.type.Predicate;

public class AttributePredicate implements Predicate {

    private String attribute;

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }
}
