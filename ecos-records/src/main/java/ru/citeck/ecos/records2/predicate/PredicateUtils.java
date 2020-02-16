package ru.citeck.ecos.records2.predicate;

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.records2.predicate.model.*;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

public class PredicateUtils {

    public static List<String> getAllPredicateAttributes(Predicate predicate) {

        List<String> result = new ArrayList<>();

        mapValuePredicatesImpl(predicate, v -> {
            result.add(v.getAttribute());
            return v;
        }, false);

        return result;
    }

    public static Optional<Predicate> filterValuePredicates(Predicate predicate,
                                                            Function<ValuePredicate, Boolean> filter) {

        Predicate res = mapValuePredicatesImpl(predicate, pred -> filter.apply(pred) ? pred : null, false);
        return Optional.ofNullable(res);
    }

    public static Predicate mapValuePredicates(Predicate predicate,
                                               Function<ValuePredicate, ValuePredicate> mapFunc) {
        return mapValuePredicatesImpl(predicate, mapFunc, false);
    }

    public static Predicate mapValuePredicates(Predicate predicate,
                                               Function<ValuePredicate, ValuePredicate> mapFunc,
                                               boolean onlyAnd) {

        return mapValuePredicatesImpl(predicate, mapFunc, onlyAnd);
    }

    public static <T> T convertToDto(Predicate predicate, Class<T> type) {
        return convertToDto(predicate, type, false);
    }

    public static <T> T convertToDto(Predicate predicate, Class<T> type, boolean onlyAnd) {

        T dto;

        try {
            dto = type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return convertToDto(predicate, dto, onlyAnd);
    }

    public static <T> T convertToDto(Predicate predicate, T dto) {
        return convertToDto(predicate, dto, false);
    }

    public static <T> T convertToDto(Predicate predicate, T dto, boolean onlyAnd) {

        Set<String> dtoFields = new HashSet<>();

        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(dto)) {
            if (!descriptor.getPropertyType().equals(Predicate.class)) {
                dtoFields.add(descriptor.getName());
            }
        }

        Map<String, Object> dtoData = new HashMap<>();

        Predicate filtered = PredicateUtils.mapValuePredicates(predicate, pred -> {

            String att = pred.getAttribute();

            if (dtoFields.contains(att)) {

                Object currentData = dtoData.get(att);
                if (currentData == null || "".equals(currentData)) {
                    dtoData.put(att, pred.getValue());
                }
                return null;
            }

            return pred;
        }, onlyAnd);

        ObjectNode dtoDataNode = JsonUtils.valueToTree(dtoData);
        JsonUtils.applyData(dtoDataNode, dto);

        try {
            PropertyUtils.setProperty(dto, "predicate", filtered);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            //do nothing
            return dto;
        }
        return dto;
    }

    private static Predicate mapValuePredicatesImpl(Predicate predicate,
                                                    Function<ValuePredicate, ValuePredicate> mapFunc,
                                                    boolean onlyAnd) {

        if (predicate == null) {
            return null;
        }

        if (predicate instanceof ValuePredicate) {

            return mapFunc.apply((ValuePredicate) predicate);

        } else if (predicate instanceof ComposedPredicate) {

            if (onlyAnd && !(predicate instanceof AndPredicate)) {
                return null;
            }

            ComposedPredicate composed = (ComposedPredicate) predicate;
            List<Predicate> mappedPredicates = new ArrayList<>();

            for (Predicate pred : composed.getPredicates()) {
                Predicate mappedPred = mapValuePredicatesImpl(pred, mapFunc, onlyAnd);
                if (mappedPred != null) {
                    mappedPredicates.add(mappedPred);
                }
            }

            if (mappedPredicates.isEmpty()) {
                return null;
            } else if (mappedPredicates.size() == 1) {
                return mappedPredicates.get(0);
            }

            if (composed instanceof AndPredicate) {
                return Predicates.and(mappedPredicates);
            } else if (composed instanceof OrPredicate) {
                return Predicates.or(mappedPredicates);
            } else {
                return null;
            }

        } else if (predicate instanceof NotPredicate) {

            NotPredicate notPred = (NotPredicate) predicate;
            Predicate mapped = mapValuePredicatesImpl(notPred.getPredicate(), mapFunc, onlyAnd);
            if (mapped != null) {
                return Predicates.not(mapped);
            } else {
                return null;
            }
        } else {
            return predicate;
        }
    }

    public static Predicate optimize(Predicate predicate) {

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
}