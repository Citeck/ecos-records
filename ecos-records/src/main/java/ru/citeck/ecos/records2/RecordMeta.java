package ru.citeck.ecos.records2;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.graphql.meta.value.RecordMetaValue;
import ru.citeck.ecos.records3.record.op.atts.RecordAtts;

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
    public RecordMeta withId(RecordRef recordRef) {
        if (getId().equals(recordRef)) {
            return this;
        }
        return new RecordMeta(recordRef, getAttributes());
    }

    @NotNull
    public RecordMeta withDefaultAppName(String appName) {
        RecordRef currId = getId();
        RecordRef newId = currId.withDefaultAppName(appName);
        return newId == currId ? this : new RecordMeta(this, newId);
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
            && Objects.equals(getAttributes(), that.getAttributes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getAttributes());
    }
}
