package ru.citeck.ecos.records2;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    private ObjectData attributes = ObjectData.create();

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

    @NotNull
    public RecordMeta withId(RecordRef recordRef) {
        if (getId().equals(recordRef)) {
            return this;
        }
        return new RecordMeta(recordRef, attributes);
    }

    @NotNull
    public RecordMeta withDefaultAppName(String appName) {
        RecordRef currId = getId();
        RecordRef newId = currId.withDefaultAppName(appName);
        return newId == currId ? this : new RecordMeta(this, newId);
    }

    @NotNull
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
        attributes.forEachJ(consumer);
    }

    @NotNull
    public ObjectData getAttributes() {
        return attributes;
    }

    public boolean has(String name) {
        return hasAttribute(name);
    }

    public boolean hasAttribute(String name) {
        return attributes.has(name);
    }

    @NotNull
    public String fmtDate(@NotNull String name, @NotNull String format) {
        return fmtDate(name, format, "");
    }

    @NotNull
    public String fmtDate(@NotNull String name, @NotNull String format, @NotNull String orElse) {
        Date date = getDateOrNull(name);
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.format(date);
        }
        return orElse;
    }

    @Nullable
    public Date getDateOrNull(String name) {
        return attributes.get(name, Date.class, null);
    }

    @Nullable
    public String getStringOrNull(String name) {
        return attributes.get(name, String.class, null);
    }

    @Nullable
    public Double getDoubleOrNull(String name) {
        return attributes.get(name, Double.class);
    }

    @Nullable
    public Boolean getBoolOrNull(String name) {
        return attributes.get(name, Boolean.class);
    }

    @NotNull
    public DataValue get(String name) {
        return getAttribute(name);
    }

    @NotNull
    public <T> T get(String name, @NotNull T orElse) {
        return getAttribute(name, orElse);
    }

    @NotNull
    public DataValue getAttribute(String name) {
        return attributes.get(name);
    }

    @NotNull
    public <T> T getAttribute(String name, @NotNull T orElse) {
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
            this.attributes = ObjectData.create();
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
