package ru.citeck.ecos.predicate.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.AttributePredicate;

public class AttRangePredicate<T> extends AttributePredicate {

    public static String MIN = "MIN";
    public static String MAX = "MAX";

    private T from;
    private T to;

    public T getFrom() {
        return from;
    }

    void setFrom(T from) {
        this.from = from;
    }

    public T getTo() {
        return to;
    }

    void setTo(T to) {
        this.to = to;
    }

    @JsonCreator
    public static AttRangePredicate<?> create(ObjectNode predicate) {

        JsonNode values = predicate.get("val");

        if (values.size() != 2) {
            throw new IllegalArgumentException("Range values have incorrect size. predicate: " + predicate);
        }

        JsonNode from = values.get(0);
        JsonNode to = values.get(1);

        AttRangePredicate<?> result = null;

        if (from.isTextual()) {

            Str str = new Str();
            str.setFrom(from.asText());
            str.setTo(to.asText());
            result = str;

        } else if (from.isNumber()) {

            Num num = new Num();
            num.setFrom(from.asDouble());
            num.setTo(to.asDouble());
            result = num;
        }

        if (result == null) {
            throw new IllegalArgumentException("Range elements type is not supported! predicate: " + predicate);
        }

        result.init(predicate);
        return result;
    }

    public static class Str extends AttRangePredicate<String> {
    }

    public static class Num extends AttRangePredicate<Double> {
    }
}
