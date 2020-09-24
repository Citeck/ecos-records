package ru.citeck.ecos.records3.graphql.meta.value;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.MissingNode;
import ecos.com.fasterxml.jackson210.databind.node.NullNode;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.resolver.AttContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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
    public Object getAttribute(@NotNull String name) {

        SchemaAtt field = AttContext.getCurrentSchemaAtt();

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
    public Object getAs(@NotNull String type) {

        SchemaAtt field = AttContext.getCurrentSchemaAtt();

        String fieldName = field.getAlias();
        if (fieldName == null) {
            fieldName = "as";
        }

        return new InnerMetaValue(value.path(fieldName));
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
    public boolean has(@NotNull String name) {
        return getScalarNode(value, "has").asBoolean();
    }

    @Override
    public String getDisplayName() {
        return getScalar(value, "disp", JsonNode::asText);
    }

    @Override
    public String getString() {
        return getScalar(value, "str", JsonNode::asText);
    }

    @Override
    public String getId() {
        return getScalar(value, "id", JsonNode::asText);
    }

    @Override
    public String getLocalId() {
        return getScalar(value, "localId", JsonNode::asText);
    }

    @Override
    public Double getDouble() {
        return getScalar(value, "num", JsonNode::asDouble);
    }

    @Override
    public Boolean getBool() {
        return getScalar(value, "bool", JsonNode::asBoolean);
    }

    @Override
    public Object getJson() {
        return getScalarNode(value, "json");
    }

    private static <T> T getScalar(JsonNode node, String name, Function<JsonNode, T> mapper) {
        JsonNode scalar = getScalarNode(node, name);
        if (scalar.isNull() || scalar.isMissingNode()) {
            return null;
        }
        return mapper.apply(scalar);
    }

    private static JsonNode getScalarNode(JsonNode node, String name) {
        if (node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            for (int i = 0; i < node.size(); i++) {
                array.add(getScalarNode(node.get(i), name));
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
            return getScalarNode(att, name);
        }
        return NullNode.getInstance();
    }
}
