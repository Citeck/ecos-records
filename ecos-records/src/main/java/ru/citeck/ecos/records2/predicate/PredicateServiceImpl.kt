package ru.citeck.ecos.records2.predicate

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.predicate.comparator.DefaultValueComparator
import ru.citeck.ecos.records2.predicate.comparator.ValueComparator
import ru.citeck.ecos.records2.predicate.element.Element
import ru.citeck.ecos.records2.predicate.element.Elements
import ru.citeck.ecos.records2.predicate.element.elematts.ElementAttributes
import ru.citeck.ecos.records2.predicate.element.raw.RawElements
import ru.citeck.ecos.records2.predicate.model.*
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import java.util.*

open class PredicateServiceImpl : PredicateService, ServiceFactoryAware {

    private lateinit var recordsService: RecordsService

    override fun <T : Any> filter(elements: Iterable<T?>, predicate: Predicate): List<T> {
        return filter(elements, predicate, Int.MAX_VALUE)
    }

    override fun <T : Any> filter(elements: Iterable<T?>, predicate: Predicate, maxElements: Int): List<T> {
        return filter(RawElements(recordsService, elements), predicate, maxElements).map { it.obj }
    }

    override fun <T : Element> filter(elements: Elements<T>, predicate: Predicate): List<T> {
        return filter(elements, predicate, Int.MAX_VALUE)
    }

    override fun <T : Element> filter(elements: Elements<T>, predicate: Predicate, maxElements: Int): List<T> {
        return filter(elements, predicate, maxElements, DefaultValueComparator)
    }

    override fun <T : Element> filter(
        elements: Elements<T>,
        predicate: Predicate,
        maxElements: Int,
        comparator: ValueComparator
    ): List<T> {
        return filterAndSort(elements, predicate, emptyList(), 0, maxElements, comparator)
    }

    override fun isMatch(element: Any?, predicate: Predicate): Boolean {
        return isMatch(element, predicate, DefaultValueComparator)
    }

    override fun isMatch(element: Any?, predicate: Predicate, comparator: ValueComparator): Boolean {
        element ?: return false
        if (element is Element) {
            return isMatch(element, predicate, comparator)
        }
        val attributes = PredicateUtils.getAllPredicateAttributes(predicate)
        val predElement = RawElements(recordsService, listOf(element))
            .getElements(attributes).firstOrNull() ?: return false
        return isMatch(predElement.getAttributes(attributes), predicate, comparator)
    }

    override fun isMatch(element: Element, predicate: Predicate): Boolean {
        return isMatch(element, predicate, DefaultValueComparator)
    }

    override fun isMatch(element: Element, predicate: Predicate, comparator: ValueComparator): Boolean {
        val attributes = PredicateUtils.getAllPredicateAttributes(predicate)
        val elemAttributes = element.getAttributes(attributes)
        return isMatch(elemAttributes, predicate, comparator)
    }

    private fun isMatch(attributes: ElementAttributes, predicate: Predicate, comparator: ValueComparator): Boolean {
        return when (predicate) {
            is ComposedPredicate -> {
                val predicates = predicate.getPredicates()
                if (predicates.isEmpty()) {
                    return true
                }
                val joinByAnd = predicate is AndPredicate
                for (innerPredicate in predicates) {
                    if (isMatch(attributes, innerPredicate, comparator)) {
                        if (!joinByAnd) {
                            return true
                        }
                    } else {
                        if (joinByAnd) {
                            return false
                        }
                    }
                }
                joinByAnd
            }
            is ValuePredicate -> {
                val value = predicate.getValue()
                val attribute = predicate.getAttribute()
                val elementValue = DataValue.create(attributes.getAttribute(attribute))
                when (predicate.getType()) {
                    ValuePredicate.Type.EQ -> comparator.isEquals(elementValue, value)
                    ValuePredicate.Type.GT -> comparator.isGreaterThan(elementValue, value, false)
                    ValuePredicate.Type.GE -> comparator.isGreaterThan(elementValue, value, true)
                    ValuePredicate.Type.LT -> comparator.isLessThan(elementValue, value, false)
                    ValuePredicate.Type.LE -> comparator.isLessThan(elementValue, value, true)
                    ValuePredicate.Type.LIKE -> comparator.isLike(elementValue, value)
                    ValuePredicate.Type.IN -> comparator.isIn(elementValue, value)
                    ValuePredicate.Type.CONTAINS -> comparator.isContains(elementValue, value)
                }
            }
            is NotPredicate -> {
                !isMatch(attributes, predicate.getPredicate(), comparator)
            }
            is EmptyPredicate -> {
                val attribute = predicate.getAttribute()
                comparator.isEmpty(DataValue.create(attributes.getAttribute(attribute)))
            }
            else -> {
                predicate is VoidPredicate
            }
        }
    }

    override fun <T : Any> filterAndSort(
        values: Iterable<T>,
        predicate: Predicate,
        sorting: List<SortBy>,
        skip: Int,
        max: Int
    ): List<T> {
        return filterAndSort(values, predicate, sorting, skip, max, DefaultValueComparator)
    }

    override fun <T : Any> filterAndSort(
        values: Iterable<T>,
        predicate: Predicate,
        sorting: List<SortBy>,
        skip: Int,
        max: Int,
        comparator: ValueComparator
    ): List<T> {
        return filterAndSort(
            RawElements(recordsService, values),
            predicate,
            sorting,
            skip,
            max,
            comparator
        ).map { it.obj }
    }

    override fun <T : Element> filterAndSort(
        elements: Elements<T>,
        predicate: Predicate,
        sorting: List<SortBy>,
        skip: Int,
        max: Int
    ): List<T> {
        return filterAndSort(elements, predicate, sorting, skip, max)
    }

    override fun <T : Element> filterAndSort(
        elements: Elements<T>,
        predicate: Predicate,
        sorting: List<SortBy>,
        skip: Int,
        max: Int,
        comparator: ValueComparator
    ): List<T> {

        if (max == 0) {
            return emptyList()
        }

        val attributesSet = hashSetOf<String>()
        sorting.forEach { attributesSet.add(it.attribute) }
        attributesSet.addAll(PredicateUtils.getAllPredicateAttributes(predicate))
        val attributes = attributesSet.toList()

        val elementsIt = elements.getElements(attributes).iterator()

        if (sorting.isEmpty()) {
            val result = mutableListOf<T>()
            forEachMatched(elementsIt, predicate, skip, max, attributes, comparator) { elem, _ ->
                result.add(elem)
            }
            return result
        }
        val attsByValue = IdentityHashMap<T, ElementAttributes>()
        val resValues = ArrayList<T>()
        forEachMatched(elementsIt, predicate, 0, -1, attributes, comparator) { elem, atts ->
            attsByValue[elem] = atts
            resValues.add(elem)
        }
        if (resValues.isEmpty()) {
            return emptyList()
        }
        val dataComparators: List<(ElementAttributes?, ElementAttributes?) -> Int> = sorting.map { sortBy ->
            val (gtValue, ltValue) = if (sortBy.ascending) {
                1 to -1
            } else {
                -1 to 1
            }
            val getDataValue: (v: Any?) -> DataValue = {
                if (it is DataValue) {
                    it
                } else {
                    DataValue.create(it)
                }
            }
            { v0: ElementAttributes?, v1: ElementAttributes? ->
                val att0 = getDataValue(v0?.getAttribute(sortBy.attribute))
                val att1 = getDataValue(v1?.getAttribute(sortBy.attribute))
                if (comparator.isGreaterThan(att0, att1, false)) {
                    gtValue
                } else if (comparator.isLessThan(att0, att1, false)) {
                    ltValue
                } else {
                    0
                }
            }
        }
        resValues.sortWith { v0, v1 ->
            val atts0 = attsByValue[v0]
            val atts1 = attsByValue[v1]
            var res = 0
            for (dataComparator in dataComparators) {
                res = dataComparator.invoke(atts0, atts1)
                if (res != 0) {
                    break
                }
            }
            res
        }
        return applyPaging(resValues, skip, max)
    }

    private inline fun <T : Element> forEachMatched(
        elements: Iterator<T>,
        predicate: Predicate,
        skip: Int,
        max: Int,
        attributes: List<String>,
        comparator: ValueComparator,
        action: (T, ElementAttributes) -> Unit
    ) {
        var toSkipCount = skip
        var toMatchCount = getMaxCountByArgValue(max)
        while (toMatchCount > 0 && elements.hasNext()) {
            val element = elements.next()
            val elemAtts = element.getAttributes(attributes)
            if (isMatch(elemAtts, predicate, comparator)) {
                if (toSkipCount > 0) {
                    toSkipCount--
                } else {
                    action.invoke(element, elemAtts)
                    toMatchCount--
                }
            }
        }
    }

    private fun <T> applyPaging(elements: List<T>, skip: Int, max: Int): List<T> {
        val maxCount = getMaxCountByArgValue(max)
        if (maxCount == 0) {
            return emptyList()
        }
        val skipCount = skip ?: 0
        if (skip != 0 || maxCount != Integer.MAX_VALUE) {
            val newRes = ArrayList<T>()
            for (i in skipCount until elements.size) {
                newRes.add(elements[i])
                if (newRes.size >= maxCount) {
                    break
                }
            }
            return newRes
        }
        return elements
    }

    private fun getMaxCountByArgValue(max: Int): Int {
        return if (max == -1) {
            Integer.MAX_VALUE
        } else {
            if (max >= 0) {
                max
            } else {
                0
            }
        }
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        this.recordsService = serviceFactory.recordsService
    }
}
