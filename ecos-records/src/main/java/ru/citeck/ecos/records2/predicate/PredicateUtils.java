package ru.citeck.ecos.records2.predicate;

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.TmplUtils;
import ru.citeck.ecos.records2.predicate.model.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

public class PredicateUtils {

    public static List<String> getAllPredicateAttributes(Predicate predicate) {

        List<String> result = new ArrayList<>();

        mapValuePredicatesImpl(predicate, v -> {
            result.add(v.getAttribute());
            DataValue value = v.getValue();
            if (value.isTextual()) {
                result.addAll(TmplUtils.getAtts(value.asText()));
            }
            return v;
        }, false);

        return result;
    }

    public static Predicate resolvePredicateWithAttributes(Predicate predicate, ObjectData attributes) {

        Predicate result = predicate.copy();

        mapValuePredicatesImpl(result, v -> {
            v.setValue(TmplUtils.applyAtts(v.getValue(), attributes));
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
                                               Function<ValuePredicate, Predicate> mapFunc) {
        return mapValuePredicatesImpl(predicate, mapFunc, false);
    }

    public static Predicate mapValuePredicates(Predicate predicate,
                                               Function<ValuePredicate, Predicate> mapFunc,
                                               boolean onlyAnd) {

        return mapValuePredicates(predicate, mapFunc, onlyAnd, true);
    }

    public static Predicate mapValuePredicates(Predicate predicate,
                                               Function<ValuePredicate, Predicate> mapFunc,
                                               boolean onlyAnd, boolean optimize) {

        return mapValuePredicatesImpl(predicate, mapFunc, onlyAnd, optimize);
    }

    public static <T> T convertToDto(Predicate predicate, Class<T> type) {
        return convertToDto(predicate, type, false);
    }

    public static <T> T convertToDto(Predicate predicate, Class<T> type, boolean onlyAnd) {

        T dto;

        try {
            dto = type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            ExceptionUtils.throwException(e);
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

        ObjectNode dtoDataNode = (ObjectNode) Json.getMapper().toJson(dtoData);
        Json.getMapper().applyData(dto, dtoDataNode);

        try {
            PropertyUtils.setProperty(dto, "predicate", filtered);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            //do nothing
            return dto;
        }
        return dto;
    }

    private static Predicate mapValuePredicatesImpl(Predicate predicate,
                                                    Function<ValuePredicate, Predicate> mapFunc,
                                                    boolean onlyAnd) {

        return mapValuePredicatesImpl(predicate, mapFunc, onlyAnd, true);
    }

    private static Predicate mapValuePredicatesImpl(Predicate predicate,
                                                    Function<ValuePredicate, Predicate> mapFunc,
                                                    boolean onlyAnd, boolean optimize) {

        if (predicate == null) {
            return null;
        }

        if (predicate instanceof ValuePredicate) {

            return mapFunc.apply((ValuePredicate) predicate);

        } else if (predicate instanceof ComposedPredicate) {

            if (onlyAnd && !(predicate instanceof AndPredicate)) {
                return predicate;
            }

            ComposedPredicate composed = (ComposedPredicate) predicate;
            List<Predicate> mappedPredicates = new ArrayList<>();

            for (Predicate pred : composed.getPredicates()) {
                Predicate mappedPred = mapValuePredicatesImpl(pred, mapFunc, onlyAnd, optimize);
                if (mappedPred != null) {
                    mappedPredicates.add(mappedPred);
                }
            }

            if (mappedPredicates.isEmpty()) {
                return null;
            } else if (optimize && mappedPredicates.size() == 1) {
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
            Predicate mapped = mapValuePredicatesImpl(notPred.getPredicate(), mapFunc, onlyAnd, optimize);
            if (mapped != null) {
                if (optimize && mapped instanceof NotPredicate) {
                    return ((NotPredicate) mapped).getPredicate();
                }
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
            List<Predicate> predicates = comp.getPredicates();
            result = comp;

            if (predicates.size() == 0) {
                return VoidPredicate.INSTANCE;
            }

            if (predicates.size() == 1) {

                result = optimize(predicates.get(0));

            } else {

                comp.setPredicates(null);

                for (Predicate child : predicates) {
                    Predicate optRes = optimize(child);
                    if (!(optRes instanceof VoidPredicate)) {
                        comp.addPredicate(optimize(child));
                    }
                }

                if (comp.getPredicates().size() == 0) {
                    result = VoidPredicate.INSTANCE;
                } else if (comp.getPredicates().size() == 1) {
                    result = comp.getPredicates().get(0);
                }
            }

            return result;

        } else if (predicate instanceof NotPredicate) {

            NotPredicate not = predicate.copy();
            if (not.getPredicate() instanceof VoidPredicate) {
                return VoidPredicate.INSTANCE;
            }
            not.setPredicate(optimize(not.getPredicate()));
            result = not;
        }

        return result;
    }
}
