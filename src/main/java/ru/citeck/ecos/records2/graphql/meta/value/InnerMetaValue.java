package ru.citeck.ecos.records2.graphql.meta.value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

public class InnerMetaValue implements MetaValue {

    private final JsonNode value;

    public InnerMetaValue(JsonNode value) {
        this.value = value;
    }

    @Override
    public Object getAttribute(String name, MetaField field) {

        String fieldName = field.getAlias();
        if (fieldName == null) {
            fieldName = field.getName();
        }

        if (fieldName != null) {

            JsonNode node = value.path(fieldName);

            if (node instanceof ArrayNode) {
                List<Object> result = new ArrayList<>();
                for (JsonNode val : node) {
                    result.add(new InnerMetaValue(val));
                }
                return result;
            } else {
                return new InnerMetaValue(node);
            }
        }

        return null;
    }

    @Override
    public String getDisplayName() {
        return value.path("disp").asText();
    }

    @Override
    public String getString() {
        return value.path("str").asText();
    }

    @Override
    public String getId() {
        return value.path("id").asText();
    }

    @Override
    public Double getDouble() {
        return value.path("num").asDouble();
    }

    @Override
    public Boolean getBool() {
        return value.path("bool").asBoolean();
    }

    @Override
    public Object getJson() {
        return value.get("json");
    }
}
