package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.predicate.model.*;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class PredicateUtils {

    private static final String DTO_ATT_PREFIX = "__";
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

        T dto;

        try {
            dto = type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return convertToDto(predicate, dto);
    }

    public static <T> T convertToDto(Predicate predicate, T dto) {

        String predicateField = null;

        Set<String> dtoFields = new HashSet<>();

        for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(dto)) {
            if (predicateField == null && descriptor.getPropertyType().equals(Predicate.class)) {
                predicateField = descriptor.getName();
            } else {
                dtoFields.add(descriptor.getName());
            }
        }

        Map<String, Object> dtoData = new HashMap<>();

        Predicate filtered = PredicateUtils.mapValuePredicates(predicate, pred -> {

            String att = pred.getAttribute();

            if (att.startsWith(DTO_ATT_PREFIX)) {

                att = att.replaceAll("^" + DTO_ATT_PREFIX, "");

                if (dtoFields.contains(att)) {
                    dtoData.put(att, pred.getValue());
                    return null;
                }
            }

            return pred;
        }, true);

        if (predicateField != null) {
            dtoData.put(predicateField, filtered);
        }

        ObjectNode dtoDataNode = MAPPER.valueToTree(dtoData);
        try {
            MAPPER.readerForUpdating(dto).readValue(dtoDataNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            }

            if (composed instanceof AndPredicate) {
                return Predicates.and(mappedPredicates);
            } else if (composed instanceof OrPredicate && !onlyAnd) {
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
