package ru.citeck.ecos.records2.predicate

import ru.citeck.ecos.records2.predicate.comparator.ValueComparator
import ru.citeck.ecos.records2.predicate.element.Element
import ru.citeck.ecos.records2.predicate.element.Elements
import ru.citeck.ecos.records2.predicate.model.Predicate

interface PredicateService {

    companion object {
        const val LANGUAGE_PREDICATE = "predicate"
    }

    fun isMatch(element: Any?, predicate: Predicate): Boolean

    fun isMatch(element: Element, predicate: Predicate): Boolean

    fun isMatch(element: Any?, predicate: Predicate, comparator: ValueComparator): Boolean

    fun isMatch(element: Element, predicate: Predicate, comparator: ValueComparator): Boolean

    fun <T : Any> filter(elements: Iterable<T?>, predicate: Predicate): List<T>

    fun <T : Any> filter(elements: Iterable<T?>, predicate: Predicate, maxElements: Int): List<T>

    fun <T : Element> filter(elements: Elements<T>, predicate: Predicate): List<T>

    fun <T : Element> filter(elements: Elements<T>, predicate: Predicate, maxElements: Int): List<T>

    fun <T : Element> filter(
        elements: Elements<T>,
        predicate: Predicate,
        maxElements: Int,
        comparator: ValueComparator
    ): List<T>
}
