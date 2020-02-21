package ru.citeck.ecos.records2.predicate;

import java.util.List;

public interface Elements<T extends Element> {

    Iterable<T> getElements(List<String> attributes);
}
