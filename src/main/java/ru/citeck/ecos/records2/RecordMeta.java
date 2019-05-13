package ru.citeck.ecos.records2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ISO8601Utils;
import ru.citeck.ecos.records2.utils.MandatoryParam;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RecordMeta {

    private RecordRef id = RecordRef.EMPTY;

    private ObjectNode attributes = JsonNodeFactory.instance.objectNode();

    public RecordMeta() {
    }

    public RecordMeta(RecordMeta other, RecordRef id) {
        setId(id);
        setAttributes(other.getAttributes());
    }

    public RecordMeta(RecordMeta other, Function<RecordRef, RecordRef> idMapper) {
        setId(idMapper.apply(other.getId()));
        setAttributes(other.getAttributes());
    }

    public RecordMeta(String id) {
        setId(id);
    }

    public RecordMeta(RecordRef id) {
        setId(id);
    }

    public RecordMeta(RecordRef id, ObjectNode attributes) {
        setId(id);
        setAttributes(attributes);
    }

    public RecordRef getId() {
        return id;
    }

    public void setId(String id) {
        this.id = RecordRef.valueOf(id);
    }

    @JsonIgnore
    public void setId(RecordRef id) {
        this.id = RecordRef.valueOf(id);
    }

    public void forEach(BiConsumer<String, JsonNode> consumer) {
        Iterator<String> names = attributes.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            consumer.accept(name, attributes.get(name));
        }
    }

    public ObjectNode getAttributes() {
        return attributes;
    }

    public boolean has(String name) {
        return hasAttribute(name);
    }

    public boolean hasAttribute(String name) {
        return !isEmpty(attributes.path(name));
    }

    public String fmtDate(String name, String format) {
        return fmtDate(name, format, "");
    }

    public String fmtDate(String name, String format, String orElse) {
        Date date = getDateOrNull(name);
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.format(date);
        }
        return orElse;
    }

    public Date getDateOrNull(String name) {
        String value = getAttribute(name, "");
        if (!value.isEmpty()) {
            return ISO8601Utils.parse(value);
        }
        return null;
    }

    public JsonNode get(String name) {
        return getAttribute(name);
    }

    public <T> T get(String name, T orElse) {
        return getAttribute(name, orElse);
    }

    public JsonNode getAttribute(String name) {
        return attributes.path(name);
    }

    public <T> T getAttribute(String name, T orElse) {

        MandatoryParam.checkString("name", name);
        MandatoryParam.check("orElse", orElse);

        JsonNode att;
        if (name.charAt(0) == '/') {
            att = attributes.at(name);
        } else {
            att = attributes.get(name);
        }
        if (isEmpty(att)) {
            return orElse;
        }

        Object value;

        if (orElse instanceof String) {
            value = att.asText();
        } else if (orElse instanceof Integer) {
            value = att.asInt((Integer) orElse);
        } else if (orElse instanceof Long) {
            value = att.asLong((Long) orElse);
        } else if (orElse instanceof Double) {
            value = att.asDouble((Double) orElse);
        } else if (orElse instanceof Float) {
            value = (float) att.asDouble((Float) orElse);
        } else if (orElse instanceof Boolean) {
            value = att.asBoolean((Boolean) orElse);
        } else if (orElse instanceof JsonNode) {
            value = att;
        } else {
            value = orElse;
        }

        @SuppressWarnings("unchecked")
        T resultValue = (T) value;

        return resultValue;
    }

    public void set(String name, Date value) {
        setAttribute(name, value);
    }

    public void set(String name, String value) {
        setAttribute(name, value);
    }

    public void set(String name, Boolean value) {
        setAttribute(name, value);
    }

    public void set(String name, JsonNode value) {
        setAttribute(name, value);
    }

    public void setAttribute(String name, Date value) {
        setAttribute(name, ISO8601Utils.format(value));
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public void setAttribute(String name, Boolean value) {
        attributes.put(name, value);
    }

    public void setAttribute(String name, JsonNode value) {
        attributes.put(name, value);
    }

    private boolean isEmpty(JsonNode value) {
        return value == null || value.isMissingNode() || value.isNull();
    }

    public void setAttributes(ObjectNode attributes) {
        if (attributes != null) {
            this.attributes = attributes.deepCopy();
        } else {
            this.attributes = JsonNodeFactory.instance.objectNode();
        }
    }

    @Override
    public String toString() {
        return "{"
                + "\"id\":\"" + id
                + "\", \"attributes\":" + attributes
            + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecordMeta that = (RecordMeta) o;
        return Objects.equals(id, that.id)
            && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, attributes);
    }
}
