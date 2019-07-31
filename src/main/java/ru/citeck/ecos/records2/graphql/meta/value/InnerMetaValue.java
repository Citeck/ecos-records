package ru.citeck.ecos.records2.graphql.meta.value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.ArrayList;
import java.util.List;

public class InnerMetaValue implements MetaValue {

    private final JsonNode value;

    public InnerMetaValue(JsonNode value) {
        if (value == null || value instanceof MissingNode) {
            this.value = NullNode.getInstance();
        } else {
            this.value = value;
        }
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
        return getScalar(value, "disp").asText();
    }

    @Override
    public String getString() {
        return getScalar(value, "str").asText();
    }

    @Override
    public String getId() {
        return getScalar(value, "id").asText();
    }

    @Override
    public Double getDouble() {
        return getScalar(value, "num").asDouble();
    }

    @Override
    public Boolean getBool() {
        return getScalar(value, "bool").asBoolean();
    }

    @Override
    public Object getJson() {
        return getScalar(value, "json");
    }

    private static JsonNode getScalar(JsonNode node, String name) {
        if (node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            for (int i = 0; i < node.size(); i++) {
                array.add(getScalar(node.get(i), name));
            }
            return array;
        }
        if (!node.isObject() || node.size() == 0) {
            return NullNode.getInstance();
        }
        if (node.has(name)) {
            return node.get(name);
        }
        JsonNode att = node.get("att");
        if (att == null) {
            att = node.get("atts");
            if (att == null) {
                att = node.get(node.fieldNames().next());
            }
        }
        if (att != null) {
            return getScalar(att, name);
        }
        return NullNode.getInstance();
    }
}