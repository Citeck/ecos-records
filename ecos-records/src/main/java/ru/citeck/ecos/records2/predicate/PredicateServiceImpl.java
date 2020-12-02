package ru.citeck.ecos.records2.predicate;

import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.predicate.comparator.DefaultValueComparator;
import ru.citeck.ecos.records2.predicate.comparator.ValueComparator;
import ru.citeck.ecos.records2.predicate.model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PredicateServiceImpl implements PredicateService {

    @Override
    public <T extends Element> List<T> filter(Elements<T> elements, Predicate predicate) {
        return filter(elements, predicate, Integer.MAX_VALUE);
    }

    @Override
    public <T extends Element> List<T> filter(Elements<T> elements, Predicate predicate, int maxElements) {
        return filter(elements, predicate, maxElements, new DefaultValueComparator());
    }

    @Override
    public <T extends Element> List<T> filter(Elements<T> elements,
                                              Predicate predicate,
                                              int maxElements,
                                              ValueComparator comparator) {

        List<String> attributes = PredicateUtils.getAllPredicateAttributes(predicate);
        Iterable<T> elementsToCheck = elements.getElements(attributes);

        List<T> result = new ArrayList<>();

        Iterator<T> elementsIterator = elementsToCheck.iterator();
        while (result.size() < maxElements && elementsIterator.hasNext()) {
            T elem = elementsIterator.next();
            if (isMatch(elem.getAttributes(attributes), predicate, comparator)) {
                result.add(elem);
            }
        }

        return result;
    }

    @Override
    public boolean isMatch(Element element, Predicate predicate) {
        return isMatch(element, predicate, new DefaultValueComparator());
    }

    @Override
    public boolean isMatch(Element element, Predicate predicate, ValueComparator comparator) {
        List<String> attributes = PredicateUtils.getAllPredicateAttributes(predicate);
        ElementAttributes elemAttributes = element.getAttributes(attributes);
        return isMatch(elemAttributes, predicate, comparator);
    }

    private boolean isMatch(ElementAttributes attributes, Predicate predicate, ValueComparator comparator) {

        if (predicate instanceof ComposedPredicate) {

            List<Predicate> predicates = ((ComposedPredicate) predicate).getPredicates();
            boolean joinByAnd = predicate instanceof AndPredicate;

            if (predicates.isEmpty()) {
                return true;
            }

            for (Predicate innerPredicate : predicates) {
                if (isMatch(attributes, innerPredicate, comparator)) {
                    if (!joinByAnd) {
                        return true;
                    }
                } else {
                    if (joinByAnd) {
                        return false;
                    }
                }
            }
            return joinByAnd;

        } else if (predicate instanceof ValuePredicate) {

            ValuePredicate valuePredicate = (ValuePredicate) predicate;
            String attribute = valuePredicate.getAttribute();
            Object value = valuePredicate.getValue();
            Object elementValue = Json.getMapper().toJava(attributes.getAttribute(attribute));

            switch (valuePredicate.getType()) {
                case EQ:
                    return comparator.isEquals(elementValue, value);
                case GT:
                    return comparator.isGreaterThan(elementValue, value, false);
                case GE:
                    return comparator.isGreaterThan(elementValue, value, true);
                case LT:
                    return comparator.isLessThan(elementValue, value, false);
                case LE:
                    return comparator.isLessThan(elementValue, value, true);
                case LIKE:
                    return comparator.isLike(elementValue, value);
                case IN:
                    return comparator.isIn(elementValue, value);
                case CONTAINS:
                    return comparator.isContains(elementValue, value);
                default:
                    return false;
            }

        } else if (predicate instanceof NotPredicate) {

            return !isMatch(attributes, ((NotPredicate) predicate).getPredicate(), comparator);

        } else if (predicate instanceof EmptyPredicate) {

            String attribute = ((EmptyPredicate) predicate).getAttribute();
            return comparator.isEmpty(Json.getMapper().toJava(attributes.getAttribute(attribute)));

        } else {
            return predicate instanceof VoidPredicate;
        }
    }
}
