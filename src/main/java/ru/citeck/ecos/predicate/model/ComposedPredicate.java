package ru.citeck.ecos.predicate.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ComposedPredicate implements Predicate {

    public enum Type {

        AND, OR;

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
    private List<Predicate> predicates;
    @JsonProperty("t")
    private Type type;

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "(" + predicates.stream()
                               .map(Object::toString)
                               .collect(Collectors.joining(" " + type + " ")) + ")";
    }

    public static List<String> getTypes() {
        return Arrays.stream(Type.values())
                     .map(Type::asString)
                     .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComposedPredicate that = (ComposedPredicate) o;
        return Objects.equals(predicates, that.predicates) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicates, type);
    }
}
