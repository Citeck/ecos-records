package ru.citeck.ecos.records2.utils.json;

import ecos.com.fasterxml.jackson210.core.JsonProcessingException;
import ecos.com.fasterxml.jackson210.databind.*;
import ecos.com.fasterxml.jackson210.databind.module.SimpleModule;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.NullNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.objdata.DataValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class JsonUtils {

    private static ObjectMapper mapper = new ObjectMapper();

    private static Map<Class<Object>, JsonDeserializer<?>> deserializers = new HashMap<>();
    private static Map<Class<Object>, JsonSerializer<?>> serializers = new HashMap<>();

    private static volatile boolean initialized = false;

    private static ObjectMapper getMapper() {

        if (initialized && mapper != null) {
            return mapper;
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        SimpleModule module = new SimpleModule("custom-ecos-module");

        deserializers.forEach(module::addDeserializer);
        serializers.forEach((type, s) -> module.addSerializer(s));

        mapper.registerModule(module);

        JsonUtils.mapper = mapper;
        initialized = true;

        return mapper;
    }

    public static void addDeserializer(JsonDeserializer<?> deserializer) {
        @SuppressWarnings("unchecked")
        Class<Object> type = (Class<Object>) deserializer.handledType();
        if (type == null) {
            log.error("Deserializer doesn't have handled type. Skip it. Type: " + deserializer.getClass());
        } else {
            deserializers.put(type, deserializer);
            initialized = false;
        }
    }

    public static void addSerializer(JsonSerializer<?> serializer) {
        @SuppressWarnings("unchecked")
        Class<Object> type = (Class<Object>) serializer.handledType();
        if (type == null) {
            log.error("Serializer doesn't have handled type. Skip it. Type: " + serializer.getClass());
        } else {
            serializers.put(type, serializer);
            initialized = false;
        }
    }

    public static void applyData(Object from, Object to) {

        if (to == null || from == null) {
            return;
        }

        ObjectNode node = convert(from, ObjectNode.class);
        try {
            getMapper().readerForUpdating(to).readValue(node);
        } catch (IOException e) {
            throw new RuntimeException("Exception", e);
        }
    }

    public static ObjectNode createObjectNode() {
        return getMapper().createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return getMapper().createArrayNode();
    }

    public static JsonNode readTree(String json) {
        try {
            return getMapper().readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends JsonNode> T valueToTree(Object value) {
        return getMapper().valueToTree(value);
    }

    public static <T> T read(byte[] json, Class<T> type) {
        try {
            return getMapper().readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return getMapper().readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json string is incorrect: " + json, e);
        }
    }

    public static <T> T convert(Object value, Class<T> type) {
        return convert(value, type, null);
    }

    public static <T> T convert(Object value, Class<T> type, T deflt) {
        if (value instanceof Optional) {
            value = ((Optional<?>) value).orElse(null);
        }
        if (value == null || "null".equals(value)) {
            return deflt;
        }
        try {
            if (value instanceof JsonNode) {

                JsonNode node = (JsonNode) value;

                if (node.isNull() || node.isMissingNode()) {
                    return deflt;
                }

                return getMapper().treeToValue(node, type);
            }
            Object result;
            if (value instanceof String) {

                if (type == Boolean.class) {

                    if (Boolean.TRUE.toString().equals(value)) {
                        result = true;
                    } else if (Boolean.FALSE.toString().equals(value)) {
                        result = false;
                    } else {
                        result = null;
                    }
                } else {

                    String valueStr = (String) value;

                    Character firstNotEmptyChar = null;
                    for (int i = 0; i < valueStr.length(); i++) {
                        char ch = valueStr.charAt(i);
                        if (ch != ' ') {
                            firstNotEmptyChar = ch;
                            break;
                        }
                    }

                    if (firstNotEmptyChar != null
                        && (firstNotEmptyChar == '{'
                            || firstNotEmptyChar == '['
                            || firstNotEmptyChar == '"')) {

                        result = getMapper().readValue(valueStr, type);
                    } else {
                        result = getMapper().readValue("\"" + valueStr + "\"", type);
                    }
                }
            } else {
                result = getMapper().convertValue(value, type);
            }

            @SuppressWarnings("unchecked")
            T resultT = (T) result;
            return resultT != null ? resultT : deflt;

        } catch (Exception e) {
            log.error("Conversion error. Type: '" + type + "' Value: '" + value + "'", e);
        }
        return deflt;
    }

    public static <T> T copy(T value) {
        if (value == null) {
            return null;
        }
        Object result;
        if (value instanceof String) {
            result = value;
        } else if (value instanceof JsonNode) {
            result = ((JsonNode) value).deepCopy();
        } else {
            ObjectMapper mapper = getMapper();
            try {
                result = mapper.treeToValue(mapper.valueToTree(value), value.getClass());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        @SuppressWarnings("unchecked")
        T resultT = (T) result;
        return resultT;
    }

    public static byte[] toBytes(Object value) {
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        if (value instanceof String) {
            return ((String) value).getBytes(StandardCharsets.UTF_8);
        }
        try {
            return mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        try {
            return getMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Json write error", e);
            return null;
        }
    }

    public static boolean isEquals(Object value0, Object value1) {

        if (value0 == value1) {
            return true;
        }
        if (value0 == null || value1 == null) {
            return false;
        }

        if (value0.getClass().equals(value1.getClass())) {
            return value0.equals(value1);
        }

        JsonNode json0 = toJson(value0);
        JsonNode json1 = toJson(value1);

        return json0.equals(json1);
    }

    public static JsonNode toJson(Object value) {
        if (value == null) {
            return NullNode.getInstance();
        }
        if (value instanceof String) {
            return TextNode.valueOf((String) value);
        }
        if (value instanceof JsonNode) {
            return (JsonNode) value;
        }
        return getMapper().valueToTree(value);
    }

    public static Object toJava(Object node) {

        if (node instanceof DataValue) {
            return ((DataValue) node).asJavaObj();
        }

        if (!(node instanceof JsonNode)) {
            return node;
        }

        JsonNode jsonNode = (JsonNode) node;

        if (jsonNode.isNull() || jsonNode.isMissingNode()) {
            return null;
        }

        try {
            return getMapper().treeToValue(jsonNode, Object.class);
        } catch (JsonProcessingException e) {
            log.error("Tree to Object.class conversion failed. Tree: " + node);
            return null;
        }
    }
}
