package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.predicate.model.*;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

public class PredicateUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

        ObjectNode dtoDataNode = MAPPER.valueToTree(dtoData);
        try {
            MAPPER.readerForUpdating(dto).readValue(dtoDataNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
}
