package ru.citeck.ecos.records2.predicate

import ru.citeck.ecos.records2.predicate.comparator.ValueComparator
import ru.citeck.ecos.records2.predicate.element.Element
import ru.citeck.ecos.records2.predicate.element.Elements
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy

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

    /**
     * max = -1 for unlimited request
     */
    fun <T : Any> filterAndSort(
        values: Iterable<T>,
        predicate: Predicate,
        sorting: List<SortBy>,
        skip: Int,
        max: Int,
        comparator: ValueComparator
    ): List<T>

    /**
     * max = -1 for unlimited request
     */
    fun <T : Any> filterAndSort(
        values: Iterable<T>,
        predicate: Predicate,
        sorting: List<SortBy>,
        skip: Int,
        max: Int
    ): List<T>

    /**
     * max = -1 for unlimited request
     */
    fun <T : Element> filterAndSort(
        elements: Elements<T>,
        predicate: Predicate,
        sorting: List<SortBy>,
        skip: Int,
        max: Int
    ): List<T>

    /**
     * max = -1 for unlimited request
     */
    fun <T : Element> filterAndSort(
        elements: Elements<T>,
        predicate: Predicate,
        sorting: List<SortBy>,
        skip: Int,
        max: Int,
        comparator: ValueComparator
    ): List<T>
}
