package ru.citeck.ecos.records2.predicate;

import ru.citeck.ecos.records2.predicate.comparator.ValueComparator;
import ru.citeck.ecos.records2.predicate.model.Predicate;

import java.util.List;

public interface PredicateService {

    String LANGUAGE_PREDICATE = "predicate";

    boolean isMatch(Element element, Predicate predicate);

    boolean isMatch(Element element, Predicate predicate, ValueComparator comparator);

    <T extends Element> List<T> filter(Elements<T> elements, Predicate predicate);

    <T extends Element> List<T> filter(Elements<T> elements, Predicate predicate, int maxElements);

    <T extends Element> List<T> filter(Elements<T> elements,
                                       Predicate predicate,
                                       int maxElements,
                                       ValueComparator comparator);
}
