package ru.citeck.ecos.records2.objdata;

import ecos.com.fasterxml.jackson210.annotation.JsonCreator;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.BooleanNode;
import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

@EqualsAndHashCode
public final class DataValue implements Iterable<DataValue> {

    public static final DataValue NULL = new DataValue((Object) null);

    private final JsonNode value;

    public DataValue(String value) {
        this.value = TextNode.valueOf(value);
    }

    public DataValue(Boolean value) {
        this.value = Boolean.TRUE.equals(value) ? BooleanNode.TRUE : BooleanNode.FALSE;
    }

    public DataValue(List<?> value) {
        this.value = JsonUtils.convert(value, ArrayNode.class);
    }

    @JsonCreator
    @com.fasterxml.jackson.annotation.JsonCreator
    public DataValue(Object value) {
        this.value = JsonUtils.toJson(value);
    }

    public DataValue copy() {
        return new DataValue(value);
    }

    public boolean isObject() {
        return value.isObject();
    }

    public boolean isValueNode() {
        return value.isValueNode();
    }
    
    public boolean isTextual() {
        return value.isTextual();
    }

    public boolean isBoolean() {
        return value.isBoolean();
    }

    public boolean isNotNull() {
        return !value.isNull() && !value.isMissingNode();
    }

    public boolean isNull() {
        return value.isNull() || value.isMissingNode();
    }

    public boolean isBinary() {
        return value.isBinary();
    }

    public boolean isPojo() {
        return value.isPojo();
    }

    public boolean isNumber() {
        return value.isNumber();
    }

    public boolean isIntegralNumber() {
        return value.isIntegralNumber();
    }

    public boolean isFloatingPointNumber() {
        return value.isFloatingPointNumber();
    }

    public boolean isShort() {
        return value.isShort();
    }

    public boolean isInt() {
        return value.isInt();
    }

    public boolean isLong() {
        return value.isLong();
    }

    public boolean isFloat() {
        return value.isFloat();
    }

    public boolean isDouble() {
        return value.isDouble();
    }

    public boolean isBigDecimal() {
        return value.isBigDecimal();
    }

    public boolean isBigInteger() {
        return value.isBigInteger();
    }

    public boolean isArray() {
        return value.isArray();
    }

    public DataValue get(int i) {
        return new DataValue(value.get(i));
    }

    public DataValue get(String name) {
        JsonNode res;
        if (name.charAt(0) == '/') {
            res = value.at(name);
        } else {
            res = value.get(name);
        }
        return new DataValue(res);
    }

    public int size() {
        return value.size();
    }

    @Override
    public Iterator<DataValue> iterator() {
        return new Iter(value);
    }

    public void forEach(BiConsumer<String, DataValue> consumer) {
        Iterator<String> names = value.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            consumer.accept(name, new DataValue(value.get(name)));
        }
    }

    public boolean canConvertToInt() {
        return value.canConvertToInt();
    }

    public boolean canConvertToLong() {
        return value.canConvertToLong();
    }

    public String textValue() {
        return value.textValue();
    }

    public byte[] binaryValue() throws IOException {
        return value.binaryValue();
    }

    public boolean booleanValue() {
        return value.booleanValue();
    }

    public Number numberValue() {
        return value.numberValue();
    }

    public short shortValue() {
        return value.shortValue();
    }

    public int intValue() {
        return value.intValue();
    }

    public long longValue() {
        return value.longValue();
    }

    public float floatValue() {
        return value.floatValue();
    }

    public double doubleValue() {
        return value.doubleValue();
    }

    public BigDecimal decimalValue() {
        return value.decimalValue();
    }

    public BigInteger bigIntegerValue() {
        return value.bigIntegerValue();
    }

    public String asText() {
        return value.asText();
    }

    public String asText(String defaultValue) {
        return value.asText(defaultValue);
    }

    public int asInt() {
        return value.asInt();
    }

    public int asInt(int defaultValue) {
        return value.asInt(defaultValue);
    }

    public long asLong() {
        return value.asLong();
    }

    public long asLong(long defaultValue) {
        return value.asLong(defaultValue);
    }

    public double asDouble() {
        return value.asDouble();
    }

    public double asDouble(double defaultValue) {
        return value.asDouble(defaultValue);
    }

    public boolean asBoolean() {
        return value.asBoolean();
    }

    public boolean asBoolean(boolean defaultValue) {
        return value.asBoolean(defaultValue);
    }

    @com.fasterxml.jackson.annotation.JsonValue
    public Object asJavaObj() {
        return JsonUtils.toJava(value);
    }

    public ObjectData asAttributes() {
        return value.isObject() ? JsonUtils.convert(value, ObjectData.class) : new ObjectData();
    }

    @JsonValue
    JsonNode asJson() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    private static class Iter implements Iterator<DataValue> {

        private Iterator<JsonNode> iterator;

        Iter(Iterable<JsonNode> iterable) {
            iterator = iterable.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public DataValue next() {
            return new DataValue(iterator.next());
        }
    }
}
