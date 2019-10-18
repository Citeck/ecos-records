package ru.citeck.ecos.predicate.model;

public interface Predicate {

    <T extends Predicate> T copy();
}
