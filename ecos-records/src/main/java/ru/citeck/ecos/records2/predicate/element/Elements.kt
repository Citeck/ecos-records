package ru.citeck.ecos.records2.predicate.element

interface Elements<T : Element> {
    fun getElements(attributes: List<String>): Iterable<T>
}
