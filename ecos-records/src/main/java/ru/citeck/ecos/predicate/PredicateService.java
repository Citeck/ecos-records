package ru.citeck.ecos.predicate;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.comparator.ValueComparator;
import ru.citeck.ecos.predicate.model.Predicate;

import java.util.List;

public interface PredicateService {

    String LANGUAGE_PREDICATE = "predicate";

    Predicate readJson(JsonNode predicate);

    Predicate readJson(String predicate);

    ObjectNode writeJson(Predicate predicate);

    boolean isMatch(Element element, Predicate predicate);

    boolean isMatch(Element element, Predicate predicate, ValueComparator comparator);

    <T extends Element> List<T> filter(Elements<T> elements, Predicate predicate);

    <T extends Element> List<T> filter(Elements<T> elements, Predicate predicate, int maxElements);

    <T extends Element> List<T> filter(Elements<T> elements,
                                       Predicate predicate,
                                       int maxElements,
                                       ValueComparator comparator);
}
