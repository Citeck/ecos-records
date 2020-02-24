package ru.citeck.ecos.records2.graphql.meta.value;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.MissingNode;
import ecos.com.fasterxml.jackson210.databind.node.NullNode;
import ru.citeck.ecos.commons.json.Json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InnerMetaValue implements MetaValue, HasCollectionView<InnerMetaValue> {

    private final JsonNode value;

    public InnerMetaValue(Object value) {
        this.value = Json.getMapper().convert(value, JsonNode.class);
    }

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
            return new InnerMetaValue(value.path(fieldName));
        }
        return null;
    }

    @Override
    public Collection<InnerMetaValue> getCollectionView() {
        if (value instanceof ArrayNode) {
            List<InnerMetaValue> result = new ArrayList<>();
            for (JsonNode val : value) {
                result.add(new InnerMetaValue(val));
            }
            return result;
        }
        return Collections.singletonList(this);
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
