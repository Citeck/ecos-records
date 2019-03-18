package ru.citeck.ecos.predicate.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

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
    public String toString() {
        return "('" + getAttribute() + "' " + type + " '" + value + "')";
    }

    public static List<String> getTypes() {
        return Arrays.stream(Type.values())
                     .map(Type::asString)
                     .collect(Collectors.toList());
    }

    public static ValuePredicate equal(String attribute, Object value) {
        ValuePredicate valuePredicate = new ValuePredicate();
        valuePredicate.setType(Type.EQ);
        valuePredicate.setAttribute(attribute);
        valuePredicate.setValue(value);
        return valuePredicate;
    }
}
