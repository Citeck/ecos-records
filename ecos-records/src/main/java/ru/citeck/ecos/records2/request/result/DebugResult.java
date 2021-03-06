package ru.citeck.ecos.records2.request.result;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;

import java.util.Iterator;

public abstract class DebugResult {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ObjectNode debug;

    protected DebugResult() {
    }

    protected DebugResult(DebugResult other) {
        debug = other.debug;
    }

    public void merge(DebugResult other) {
        if (other.debug == null) {
            return;
        }
        if (debug == null) {
            debug = other.debug;
            return;
        }
        debug = getNotNullDebug();

        Iterator<String> names = other.debug.fieldNames();

        while (names.hasNext()) {

            String name = names.next();

            JsonNode newValue = other.debug.path(name);
            JsonNode currValue = debug.path(name);

            if (currValue.isObject() && newValue.isObject()) {
                ((ObjectNode) currValue).putAll((ObjectNode) newValue);
            } else {
                debug.put(name, newValue);
            }
        }
    }

    public void setDebugInfo(Class<?> clazz, String key, String value) {
        setDebugInfo(clazz.getSimpleName(), key, value);
    }

    public void setDebugInfo(Class<?> clazz, String key, Object value) {
        setDebugInfo(clazz.getSimpleName(), key, String.valueOf(value));
    }

    public void setDebugInfo(String sourceKey, String key, String value) {
        ObjectNode debug = getNotNullDebug();
        JsonNode sourceNode = debug.get(sourceKey);
        ObjectNode target;
        if (sourceNode == null || !sourceNode.isObject()) {
            target = JsonNodeFactory.instance.objectNode();
            debug.put(sourceKey, target);
        } else {
            target = (ObjectNode) sourceNode;
        }
        target.put(key, TextNode.valueOf(value));
    }

    public void setDebugInfo(String key, String value) {
        setDebugInfo(key, TextNode.valueOf(value));
    }

    public void setDebugInfo(String key, JsonNode value) {
        getNotNullDebug().set(key, value);
    }

    private ObjectNode getNotNullDebug() {
        if (debug == null) {
            debug = Json.getMapper().newObjectNode();
        }
        return debug;
    }

    public ObjectData getDebug() {
        return Json.getMapper().convert(debug, ObjectData.class);
    }

    public void setDebug(ObjectData attributes) {
        JsonNode node = Json.getMapper().toJson(attributes);
        if (node instanceof ObjectNode) {
            debug = (ObjectNode) node;
        } else {
            debug = Json.getMapper().newObjectNode();
        }
    }
}
