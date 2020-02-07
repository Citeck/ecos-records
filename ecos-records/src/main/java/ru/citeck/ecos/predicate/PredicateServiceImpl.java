package ru.citeck.ecos.predicate;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import ru.citeck.ecos.predicate.comparator.DefaultValueComparator;
import ru.citeck.ecos.predicate.comparator.ValueComparator;
import ru.citeck.ecos.predicate.json.JsonConverter;
import ru.citeck.ecos.predicate.json.std.StdJsonConverter;
import ru.citeck.ecos.predicate.model.*;
import ru.citeck.ecos.records2.utils.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PredicateServiceImpl implements PredicateService {

    private volatile JsonConverter jsonConverter;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Predicate readJson(JsonNode predicateNode) {

        if (predicateNode == null) {
            return null;
        }

        JsonNode objNode = predicateNode;

        if (objNode.isTextual()) {
            try {
                objNode = objectMapper.readTree(objNode.asText());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (objNode instanceof ObjectNode) {
            return getJsonConverter().fromJson((ObjectNode) objNode);
        }

        return null;
    }

    @Override
    public Predicate readJson(String predicateJson) {
        return readJson(TextNode.valueOf(predicateJson));
    }

    @Override
    public ObjectNode writeJson(Predicate predicate) {
        return getJsonConverter().toJson(optimize(predicate));
    }

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
            Object elementValue = toJava(attributes.getAttribute(attribute));

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
            return comparator.isEmpty(toJava(attributes.getAttribute(attribute)));
        }

        return false;
    }

    private Object toJava(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode) {
            return JsonUtils.toJava((JsonNode) value);
        }
        return value;
    }

    private Predicate optimize(Predicate predicate) {

        Predicate result = predicate;

        if (predicate instanceof ComposedPredicate) {

            ComposedPredicate comp = predicate.copy();
            result = comp;

            if ((comp instanceof AndPredicate
                    || comp instanceof OrPredicate) && comp.getPredicates().size() == 1) {

                result = optimize(comp.getPredicates().get(0));

            } else {

                List<Predicate> predicatesToOptimize = comp.getPredicates();
                comp.setPredicates(null);

                for (Predicate child : predicatesToOptimize) {
                    comp.addPredicate(optimize(child));
                }
            }

            return result;

        } else if (predicate instanceof NotPredicate) {

            NotPredicate not = predicate.copy();
            not.setPredicate(optimize(not.getPredicate()));
            result = not;
        }

        return result;
    }

    private JsonConverter getJsonConverter() {
        if (jsonConverter == null) {
            synchronized (this) {
                if (jsonConverter == null) {
                    jsonConverter = new StdJsonConverter();
                }
            }
        }
        return jsonConverter;
    }

    public void setJsonConverter(JsonConverter jsonConverter) {
        synchronized (this) {
            this.jsonConverter = jsonConverter;
        }
    }
}
