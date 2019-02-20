package ru.citeck.ecos.records2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.Objects;

public class RecordRef {

    public static final RecordRef EMPTY = new RecordRef();

    public static final String SOURCE_DELIMITER = "@";
    private static final String DEFAULT_SOURCE_ID = "";

    private final String sourceId;
    private final String id;

    private RecordRef() {
        id = "";
        sourceId = "";
    }

    public RecordRef(String sourceId, String id) {
        this.sourceId = sourceId;
        this.id = id != null ? id : "";
    }

    public RecordRef(String sourceId, RecordRef id) {
        this.sourceId = sourceId;
        this.id = id.toString();
    }

    public RecordRef(String id) {
        int sourceDelimIdx = id.indexOf(SOURCE_DELIMITER);
        if (sourceDelimIdx != -1) {
            sourceId = id.substring(0, sourceDelimIdx);
            this.id = id.substring(sourceDelimIdx + 1);
        } else {
            this.sourceId = DEFAULT_SOURCE_ID;
            this.id = id;
        }
    }

    @JsonCreator
    public static RecordRef valueOf(String id) {
        if (StringUtils.isBlank(id)) {
            return EMPTY;
        }
        return new RecordRef(id);
    }

    public static RecordRef valueOf(RecordRef ref) {
        return ref != null ? ref : EMPTY;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecordRef that = (RecordRef) o;
        return Objects.equals(sourceId, that.sourceId)
            && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(sourceId);
        result = 31 * result + Objects.hashCode(id);
        return result;
    }

    @JsonValue
    @Override
    public String toString() {
        if (this == EMPTY) {
            return "";
        }
        if (StringUtils.isBlank(sourceId)) {
            return id;
        } else {
            return sourceId + SOURCE_DELIMITER + id;
        }
    }
}
