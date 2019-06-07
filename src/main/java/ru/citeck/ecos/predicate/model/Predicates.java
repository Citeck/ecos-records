package ru.citeck.ecos.predicate.model;

public class Predicates {

    public static AndPredicate and(Predicate... predicates) {
        return AndPredicate.of(predicates);
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

    public static ValuePredicate equal(String attribute, Object value) {
        return ValuePredicate.equal(attribute, value);
    }

    public static ValuePredicate gt(String attribute, int value) {
        ValuePredicate predicate = new ValuePredicate();
        predicate.setType(ValuePredicate.Type.GT);
        predicate.setAttribute(attribute);
        predicate.setValue(value);
        return predicate;
    }

    public static ValuePredicate ge(String attribute, int value) {
        ValuePredicate predicate = new ValuePredicate();
        predicate.setType(ValuePredicate.Type.GE);
        predicate.setAttribute(attribute);
        predicate.setValue(value);
        return predicate;
    }

    public static ValuePredicate lt(String attribute, int value) {
        ValuePredicate predicate = new ValuePredicate();
        predicate.setType(ValuePredicate.Type.LT);
        predicate.setAttribute(attribute);
        predicate.setValue(value);
        return predicate;
    }

    public static ValuePredicate le(String attribute, int value) {
        ValuePredicate predicate = new ValuePredicate();
        predicate.setType(ValuePredicate.Type.LE);
        predicate.setAttribute(attribute);
        predicate.setValue(value);
        return predicate;
    }
}
