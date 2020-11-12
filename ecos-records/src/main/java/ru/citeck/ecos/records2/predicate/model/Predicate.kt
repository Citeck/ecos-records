package ru.citeck.ecos.records2.predicate.model;

public interface Predicate {

    <T extends Predicate> T copy();
}
