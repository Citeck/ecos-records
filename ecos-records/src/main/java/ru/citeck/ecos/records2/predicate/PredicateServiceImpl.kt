package ru.citeck.ecos.records2.predicate

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records2.predicate.comparator.DefaultValueComparator
import ru.citeck.ecos.records2.predicate.comparator.ValueComparator
import ru.citeck.ecos.records2.predicate.element.Element
import ru.citeck.ecos.records2.predicate.element.Elements
import ru.citeck.ecos.records2.predicate.element.elematts.ElementAttributes
import ru.citeck.ecos.records2.predicate.element.elematts.RecordAttsElement
import ru.citeck.ecos.records2.predicate.element.raw.RawElements
import ru.citeck.ecos.records2.predicate.model.*
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import java.util.*

open class PredicateServiceImpl(factory: RecordsServiceFactory) : PredicateService {

    private val recordsService: RecordsService = factory.recordsServiceV1

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
        return filter(elements, predicate, maxElements, DefaultValueComparator())
    }

    override fun <T : Element> filter(
        elements: Elements<T>,
        predicate: Predicate,
        maxElements: Int,
        comparator: ValueComparator
    ): List<T> {

        val attributes = PredicateUtils.getAllPredicateAttributes(predicate)
        val elementsToCheck = elements.getElements(attributes)
        val result: MutableList<T> = ArrayList()
        val elementsIterator = elementsToCheck.iterator()
        while (result.size < maxElements && elementsIterator.hasNext()) {
            val elem = elementsIterator.next()
            if (isMatch(elem.getAttributes(attributes), predicate, comparator)) {
                result.add(elem)
            }
        }
        return result
    }

    override fun isMatch(element: Any?, predicate: Predicate): Boolean {
        return isMatch(element, predicate, DefaultValueComparator())
    }

    override fun isMatch(element: Any?, predicate: Predicate, comparator: ValueComparator): Boolean {
        element ?: return false
        val attributes = PredicateUtils.getAllPredicateAttributes(predicate)
        val elemAttributes = RecordAttsElement(element, recordsService.getAtts(element, attributes))
        return isMatch(elemAttributes.getAttributes(attributes), predicate, comparator)
    }

    override fun isMatch(element: Element, predicate: Predicate): Boolean {
        return isMatch(element, predicate, DefaultValueComparator())
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
}
