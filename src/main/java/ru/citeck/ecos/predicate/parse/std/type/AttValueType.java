package ru.citeck.ecos.predicate.parse.std.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.AttributePredicate;

import java.util.ArrayList;
import java.util.List;

public abstract class AttValueType extends AttributePredicate implements StdPredicateType {

    public enum CompareType {
        EQ, GT, GE, LT, LE, LIKE, IN
    }

    private T value;
    private CompareType type;

    public T getValue() {
        return value;
    }

    void setValue(T value) {
        this.value = value;
    }

    public CompareType getType() {
        return type;
    }

    void setType(CompareType type) {
        this.type = type;
    }

    @JsonCreator
    public static AttValueType<?> create(ObjectNode value) {

        JsonNode nodeValue = value.path("val");
        AttValueType<?> result = null;

        if (nodeValue.isTextual()) {
            Str str = new Str();
            str.setValue(nodeValue.asText());
            result = str;
        }

        if (nodeValue.isNumber()) {
            Num num = new Num();
            num.setValue(nodeValue.asDouble());
            result = num;
        }

        if (nodeValue.isBoolean()) {
            Bool bool = new Bool();
            bool.setValue(nodeValue.asBoolean());
            result = bool;
        }

        if (nodeValue.isArray()) {

            if (nodeValue.size() == 0) {
                throw new IllegalArgumentException("List value can't be empty. " + value);
            }

            if (nodeValue.get(0).isTextual()) {

                StrList strList = new StrList();
                List<String> listValue = new ArrayList<>();
                for (JsonNode node : nodeValue) {
                    listValue.add(node.asText());
                }
                strList.setValue(listValue);
                result = strList;

            } else if (nodeValue.get(0).isNumber()) {

                NumList numList = new NumList();
                List<Double> listValue = new ArrayList<>();
                for (JsonNode node : nodeValue) {
                    listValue.add(node.asDouble());
                }
                numList.setValue(listValue);
                result = numList;

            } else {

                throw new IllegalArgumentException("List value type is incorrect. " + value);
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Value type is not supported! " +
                                               nodeValue.getClass() + " " + nodeValue);
        }

        String type = value.get("t").asText();
        result.setType(CompareType.valueOf(type.toUpperCase()));
        result.init(value);

        return result;
    }

    public static class Str extends AttValueType<String> {
    }

    public static class Num extends AttValueType<Double> {
    }

    public static class Bool extends AttValueType<Boolean> {
    }

    public static class StrList extends AttValueType<List<String>> {
    }

    public static class NumList extends AttValueType<List<Double>> {
    }
}
