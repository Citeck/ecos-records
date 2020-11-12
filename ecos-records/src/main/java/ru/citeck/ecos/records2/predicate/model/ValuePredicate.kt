package ru.citeck.ecos.records2.predicate.model;

import ecos.com.fasterxml.jackson210.annotation.JsonCreator;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ValuePredicate extends AttributePredicate {

    public enum Type {

        EQ, GT, GE, LT, LE, LIKE, IN, CONTAINS;

        @JsonValue
        public String asString() {
            return name().toLowerCase();
        }

        @JsonCreator
        public static Type fromString(String type) {
            return Type.valueOf(type.toUpperCase());
        }
    }

    @JsonProperty("val")
    private Object value;
    @JsonProperty("t")
    private Type type = Type.EQ;

    public ValuePredicate() {
    }

    public ValuePredicate(String attribute, Type type, Object value) {
        this.setAttribute(attribute);
        this.value = value;
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public <T extends Predicate> T copy() {

        ValuePredicate predicate = new ValuePredicate();
        predicate.setAttribute(getAttribute());
        predicate.setType(getType());
        predicate.setValue(getValue());

        @SuppressWarnings("unchecked")
        T result = (T) predicate;

        return result;
    }

    @Override
    public String toString() {
        return "('" + getAttribute() + "' " + type + " '" + value + "')";
    }

    public static List<String> getTypes() {
        return Arrays.stream(Type.values())
                     .map(Type::asString)
                     .collect(Collectors.toList());
    }

    public static ValuePredicate equal(String attribute, Object value) {
        return new ValuePredicate(attribute, Type.EQ, value);
    }

    public static ValuePredicate eq(String attribute, Object value) {
        return equal(attribute, value);
    }

    public static ValuePredicate contains(String attribute, Object value) {
        return new ValuePredicate(attribute, Type.CONTAINS, value);
    }

    public static ValuePredicate like(String attribute, Object value) {
        return new ValuePredicate(attribute, Type.LIKE, value);
    }

    public static ValuePredicate gt(String attribute, Object value) {
        return new ValuePredicate(attribute, Type.GT, value);
    }

    public static ValuePredicate ge(String attribute, Object value) {
        return new ValuePredicate(attribute, Type.GE, value);
    }

    public static ValuePredicate lt(String attribute, Object value) {
        return new ValuePredicate(attribute, Type.LT, value);
    }

    public static ValuePredicate le(String attribute, Object value) {
        return new ValuePredicate(attribute, Type.LE, value);
    }


}
