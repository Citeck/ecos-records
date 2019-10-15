package ru.citeck.ecos.predicate;

import java.util.List;

public interface Elements<T extends Element> {

    Iterable<T> getElements(List<String> attributes);
}
