package ru.citeck.ecos.records3.record.operation.meta.value.impl;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.MissingNode;
import ecos.com.fasterxml.jackson210.databind.node.NullNode;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordConstants;
import ru.citeck.ecos.records3.record.operation.meta.value.HasCollectionView;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class InnerMetaValue implements AttValue, HasCollectionView<InnerMetaValue> {

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
    public Object getAtt(@NotNull String name) {

        JsonNode node = value.path(name);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new InnerMetaValue(node);
    }

    @Override
    public Object getAs(@NotNull String type) {

        JsonNode node = value.path(RecordConstants.ATT_AS).path(type);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }

        return new InnerMetaValue(node);
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

        JsonNode node = value.path(RecordConstants.ATT_HAS)
            .path(name)
            .path("?bool");

        if (node.isMissingNode() || node.isNull()) {
            return false;
        }
        return node.asBoolean();
    }

    @Override
    public String getDisplayName() {
        return getScalar(value, "?disp", JsonNode::asText);
    }

    @Override
    public String getString() {
        return getScalar(value, "?str", JsonNode::asText);
    }

    @Override
    public String getId() {
        return getScalar(value, "?id", JsonNode::asText);
    }

    @Override
    public Double getDouble() {
        return getScalar(value, "?num", JsonNode::asDouble);
    }

    @Override
    public Boolean getBool() {
        return getScalar(value, "?bool", JsonNode::asBoolean);
    }

    @Override
    public Object getJson() {
        return getScalar(value, "?json", n -> n);
    }

    private static <T> T getScalar(JsonNode node, String name, Function<JsonNode, T> mapper) {
        JsonNode scalar = node.path(name);
        if (scalar.isNull() || scalar.isMissingNode()) {
            return null;
        }
        return mapper.apply(scalar);
    }
}
