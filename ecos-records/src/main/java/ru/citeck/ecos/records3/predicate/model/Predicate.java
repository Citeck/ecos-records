package ru.citeck.ecos.records3.predicate.model;

public interface Predicate {

    <T extends Predicate> T copy();
}
