package ru.citeck.ecos.records2.request.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

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
            debug = JsonNodeFactory.instance.objectNode();
        }
        return debug;
    }

    public ObjectNode getDebug() {
        return debug;
    }

    public void setDebug(ObjectNode debug) {
        this.debug = debug;
    }
}
