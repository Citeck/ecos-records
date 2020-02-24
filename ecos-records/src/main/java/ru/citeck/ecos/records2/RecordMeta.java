package ru.citeck.ecos.records2;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
public class RecordMeta {

    private RecordRef id = RecordRef.EMPTY;

    private ObjectData attributes = new ObjectData();

    public RecordMeta() {
    }

    public RecordMeta(RecordMeta other) {
        setId(other.getId());
        setAttributes(other.getAttributes());
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

    public RecordMeta(RecordRef id, ObjectData attributes) {
        setId(id);
        setAttributes(attributes);
    }

    public RecordMeta withId(RecordRef recordRef) {
        if (getId().equals(recordRef)) {
            return this;
        }
        return new RecordMeta(recordRef, attributes);
    }

    public RecordMeta withDefaultAppName(String appName) {
        RecordRef currId = getId();
        RecordRef newId = currId.withDefaultAppName(appName);
        return newId == currId ? this : new RecordMeta(this, newId);
    }

    public RecordRef getId() {
        return id;
    }

    @JsonProperty
    public void setId(String id) {
        this.id = RecordRef.valueOf(id);
    }

    public void setId(RecordRef id) {
        this.id = RecordRef.valueOf(id);
    }

    public void forEach(BiConsumer<String, DataValue> consumer) {
        attributes.forEach(consumer);
    }

    public ObjectData getAttributes() {
        return attributes;
    }

    public boolean has(String name) {
        return hasAttribute(name);
    }

    public boolean hasAttribute(String name) {
        return attributes.has(name);
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
        return attributes.get(name, Date.class, null);
    }

    public String getStringOrNull(String name) {
        return attributes.get(name, String.class, null);
    }

    public Double getDoubleOrNull(String name) {
        return attributes.get(name, Double.class);
    }

    public Boolean getBoolOrNull(String name) {
        return attributes.get(name, Boolean.class);
    }

    public DataValue get(String name) {
        return getAttribute(name);
    }

    public <T> T get(String name, T orElse) {
        return getAttribute(name, orElse);
    }

    public DataValue getAttribute(String name) {
        return attributes.get(name);
    }

    public <T> T getAttribute(String name, T orElse) {
        return attributes.get(name, orElse);
    }

    public void set(String name, Object value) {
        setAttribute(name, value);
    }

    public void setAttribute(String name, Object value) {
        attributes.set(name, value);
    }

    public void setAttributes(ObjectData attributes) {
        if (attributes != null) {
            this.attributes = attributes.deepCopy();
        } else {
            this.attributes = new ObjectData();
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
