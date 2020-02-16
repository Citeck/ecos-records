package ru.citeck.ecos.records2.attributes;

import ecos.com.fasterxml.jackson210.annotation.JsonCreator;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import ru.citeck.ecos.records2.utils.MandatoryParam;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class Attributes {

    private final Map<String, AttValue> data;

    public Attributes() {
        data = new HashMap<>();
    }

    @JsonCreator
    @com.fasterxml.jackson.annotation.JsonCreator
    public Attributes(Object data) {
        if (data == null) {
            this.data = new HashMap<>();
        } else {
            this.data = JsonUtils.convert(data, AttsMap.class);
        }
    }

    public void forEach(BiConsumer<String, AttValue> consumer) {
        data.forEach(consumer);
    }

    public boolean has(String name) {
        return innerGet(name).isNotNull();
    }

    public AttValue remove(String name) {
        AttValue prev = data.remove(name);
        return prev != null ? prev : AttValue.NULL;
    }

    public AttValue set(String name, Object value) {
        AttValue prev = data.put(name, JsonUtils.convert(value, AttValue.class));
        return prev != null ? prev : AttValue.NULL;
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

    public AttValue get(String field) {
        return innerGet(field);
    }

    private AttValue innerGet(String field) {
        AttValue att;
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
        return att != null ? att : AttValue.NULL;
    }

    @JsonValue
    @com.fasterxml.jackson.annotation.JsonValue
    public Map<String, AttValue> getData() {
        return Collections.unmodifiableMap(data);
    }

    public int size() {
        return data.size();
    }

    public Attributes copy() {
        return new Attributes(data);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Attributes that = (Attributes) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    public static class AttsMap extends HashMap<String, AttValue> {}
}
