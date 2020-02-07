package ru.citeck.ecos.records2.utils;

import ecos.com.fasterxml.jackson210.databind.JsonNode;

import java.io.Serializable;
import java.util.ArrayList;

public class JsonUtils {

    public static Serializable toJava(JsonNode node) {

        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        if (node.isArray()) {

            ArrayList<Serializable> values = new ArrayList<>();

            for (JsonNode subNode : node) {
                values.add(toJava(subNode));
            }

            return values;

        } else if (node.isNumber()) {

            return node.isIntegralNumber() ? node.asLong() : node.asDouble();

        } else if (node.isBoolean()) {

            return node.asBoolean();
        }

        return node.asText();
    }
}
