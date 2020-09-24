package ru.citeck.ecos.records3.predicate;

import java.util.List;

public interface Elements<T extends Element> {

    Iterable<T> getElements(List<String> attributes);
}
