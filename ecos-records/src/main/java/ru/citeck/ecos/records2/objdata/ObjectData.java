package ru.citeck.ecos.records2.objdata;

import ecos.com.fasterxml.jackson210.annotation.JsonCreator;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import ru.citeck.ecos.records2.utils.MandatoryParam;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class ObjectData {

    private final Map<String, DataValue> data;

    public ObjectData() {
        data = new HashMap<>();
    }

    @JsonCreator
    @com.fasterxml.jackson.annotation.JsonCreator
    public ObjectData(Object data) {
        if (data == null) {
            this.data = new HashMap<>();
        } else {
            this.data = JsonUtils.convert(data, DataMap.class);
        }
    }

    public void forEach(BiConsumer<String, DataValue> consumer) {
        data.forEach(consumer);
    }

    public boolean has(String name) {
        return innerGet(name).isNotNull();
    }

    public DataValue remove(String name) {
        DataValue prev = data.remove(name);
        return prev != null ? prev : DataValue.NULL;
    }

    public DataValue set(String name, Object value) {
        DataValue prev = data.put(name, JsonUtils.convert(value, DataValue.class));
        return prev != null ? prev : DataValue.NULL;
    }

    public <T> T get(String field, Class<T> type, T orElse) {
        return JsonUtils.convert(innerGet(field), type, orElse);
    }

    public <T> T get(String field, Class<T> type) {
        return get(field, type, null);
    }

    public <T> T get(String field, T orElse) {

        MandatoryParam.checkString("name", field);
        MandatoryParam.check("orElse", orElse);

        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) orElse.getClass();

        return get(field, type, orElse);
    }

    public DataValue get(String field) {
        return innerGet(field);
    }

    private DataValue innerGet(String field) {
        DataValue att;
        if (field.charAt(0) == '/') {
            int slashIdx = field.indexOf('/', 1);
            if (slashIdx == -1) {
                att = data.get(field.substring(1));
            } else {
                att = data.get(field.substring(1, slashIdx));
                att = att.get(field.substring(slashIdx));
            }
        } else {
            att = data.get(field);
        }
        return att != null ? att : DataValue.NULL;
    }

    @JsonValue
    @com.fasterxml.jackson.annotation.JsonValue
    public Map<String, DataValue> getData() {
        return Collections.unmodifiableMap(data);
    }

    public int size() {
        return data.size();
    }

    public ObjectData copy() {
        return new ObjectData(data);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObjectData that = (ObjectData) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    public static class DataMap extends HashMap<String, DataValue> {}
}
