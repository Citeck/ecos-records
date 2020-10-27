package ru.citeck.ecos.records2;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;

import java.util.Objects;
import java.util.function.Function;

/**
 * @deprecated -> RecordAtts
 */
@Slf4j
@Deprecated
public class RecordMeta extends RecordAtts {

    public RecordMeta() {
    }

    public RecordMeta(RecordMeta other) {
        super(other);
    }

    public RecordMeta(RecordMeta other, RecordRef id) {
        super(other, id);
    }

    public RecordMeta(RecordMeta other, Function<RecordRef, RecordRef> idMapper) {
        super(other, idMapper);
    }

    public RecordMeta(String id) {
        super(id);
    }

    public RecordMeta(RecordRef id) {
        super(id);
    }

    public RecordMeta(RecordRef id, ObjectData attributes) {
        super(id, attributes);
    }

    public RecordMeta(RecordAtts atts) {
        super(atts);
    }

    @NotNull
    public RecordMeta withId(@NotNull RecordRef recordRef) {
        if (getId().equals(recordRef)) {
            return this;
        }
        return new RecordMeta(recordRef, getAtts());
    }

    @NotNull
    public RecordMeta withDefaultAppName(@NotNull String appName) {
        RecordRef currId = getId();
        RecordRef newId = currId.withDefaultAppName(appName);
        return newId == currId ? this : new RecordMeta(this, newId);
    }

    public boolean has(@Nullable String att) {
        if (att == null) {
            return false;
        }
        return hasAtt(att);
    }

    public void setAttributes(@Nullable ObjectData attributes) {
        if (attributes == null) {
            setAtts(ObjectData.create());
        } else {
            setAtts(attributes);
        }
    }

    @NotNull
    public ObjectData getAttributes() {
        return getAtts();
    }

    @NotNull
    public DataValue getAttribute(@NotNull String name) {
        return get(name);
    }

    @NotNull
    public <T> T getAttribute(@NotNull String name, @NotNull T deflt) {
        return get(name, deflt);
    }

    public void setAttribute(@NotNull String att, @Nullable Object value) {
        set(att, value);
    }

    public <T> T get(@NotNull String name, @NotNull T deflt) {
        return getAtt(name, deflt);
    }

    @NotNull
    public DataValue get(@NotNull String name) {
        return getAtt(name);
    }

    public void set(@NotNull String name, @Nullable Object value) {
        getAttributes().set(name, value);
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
        return Objects.equals(getId(), that.getId())
            && Objects.equals(getAtts(), that.getAtts());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getAtts());
    }
}
