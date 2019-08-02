package ru.citeck.ecos.predicate;

import ru.citeck.ecos.predicate.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class PredicateUtils {

    public static Optional<Predicate> filterValuePredicates(Predicate predicate,
                                                            Function<ValuePredicate, Boolean> filter) {

        return Optional.ofNullable(mapValuePredicatesImpl(predicate, pred -> filter.apply(pred) ? pred : null));
    }

    public static Predicate mapValuePredicates(Predicate predicate,
                                               Function<ValuePredicate, ValuePredicate> mapFunc) {

        return mapValuePredicatesImpl(predicate, mapFunc);
    }

    private static Predicate mapValuePredicatesImpl(Predicate predicate,
                                                    Function<ValuePredicate, ValuePredicate> mapFunc) {

        if (predicate == null) {
            return null;
        }

        if (predicate instanceof ValuePredicate) {

            return mapFunc.apply((ValuePredicate) predicate);

        } else if (predicate instanceof ComposedPredicate) {

            ComposedPredicate composed = (ComposedPredicate) predicate;
            List<Predicate> mappedPredicates = new ArrayList<>();

            for (Predicate pred : composed.getPredicates()) {
                Predicate mappedPred = mapValuePredicatesImpl(pred, mapFunc);
                if (mappedPred != null) {
                    mappedPredicates.add(mappedPred);
                }
            }

            if (mappedPredicates.isEmpty()) {
                return null;
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
            Predicate mapped = mapValuePredicatesImpl(notPred.getPredicate(), mapFunc);
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
