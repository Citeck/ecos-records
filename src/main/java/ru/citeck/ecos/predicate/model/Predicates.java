package ru.citeck.ecos.predicate.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class Predicates {

    public static ValuePredicate in(String attribute, Collection<String> values) {
        return new ValuePredicate(attribute, ValuePredicate.Type.IN, new ArrayList<>(values));
    }

    public static ValuePredicate contains(String attribute, String substring) {
        return ValuePredicate.contains(attribute, substring);
    }

    public static AndPredicate and(Predicate... predicates) {
        return AndPredicate.of(predicates);
    }

    public static AndPredicate and(Collection<Predicate> predicates) {
        AndPredicate and = new AndPredicate();
        and.setPredicates(new ArrayList<>(predicates));
        return and;
    }

    public static OrPredicate or(Collection<Predicate> predicates) {
        OrPredicate or = new OrPredicate();
        or.setPredicates(new ArrayList<>(predicates));
        return or;
    }

    public static OrPredicate or(Predicate... predicates) {
        return OrPredicate.of(predicates);
    }

    public static NotPredicate not(Predicate predicate) {
        return new NotPredicate(predicate);
    }

    public static NotPredicate notEmpty(String attribute) {
        return not(empty(attribute));
    }

    public static EmptyPredicate empty(String attribute) {
        return new EmptyPredicate(attribute);
    }

    public static ValuePredicate eq(String attribute, Object value) {
        return equal(attribute, value);
    }

    public static ValuePredicate equal(String attribute, Object value) {
        return ValuePredicate.equal(attribute, value);
    }

    public static ValuePredicate gt(String attribute, Instant value) {
        return ValuePredicate.gt(attribute, value);
    }

    public static ValuePredicate gt(String attribute, OffsetDateTime value) {
        return ValuePredicate.gt(attribute, value);
    }

    public static ValuePredicate gt(String attribute, Date value) {
        return ValuePredicate.gt(attribute, value);
    }

    public static ValuePredicate gt(String attribute, double value) {
        return ValuePredicate.gt(attribute, value);
    }

    public static ValuePredicate ge(String attribute, OffsetDateTime value) {
        return ValuePredicate.ge(attribute, value);
    }

    public static ValuePredicate ge(String attribute, Instant value) {
        return ValuePredicate.ge(attribute, value);
    }

    public static ValuePredicate ge(String attribute, Date value) {
        return ValuePredicate.ge(attribute, value);
    }

    public static ValuePredicate ge(String attribute, double value) {
        return ValuePredicate.ge(attribute, value);
    }

    public static ValuePredicate lt(String attribute, OffsetDateTime value) {
        return ValuePredicate.lt(attribute, value);
    }

    public static ValuePredicate lt(String attribute, Instant value) {
        return ValuePredicate.lt(attribute, value);
    }

    public static ValuePredicate lt(String attribute, Date value) {
        return ValuePredicate.lt(attribute, value);
    }

    public static ValuePredicate lt(String attribute, double value) {
        return ValuePredicate.lt(attribute, value);
    }

    public static ValuePredicate le(String attribute, Instant value) {
        return ValuePredicate.le(attribute, value);
    }

    public static ValuePredicate le(String attribute, OffsetDateTime value) {
        return ValuePredicate.le(attribute, value);
    }

    public static ValuePredicate le(String attribute, Date value) {
        return ValuePredicate.le(attribute, value);
    }

    public static ValuePredicate le(String attribute, double value) {
        return ValuePredicate.le(attribute, value);
    }
}
