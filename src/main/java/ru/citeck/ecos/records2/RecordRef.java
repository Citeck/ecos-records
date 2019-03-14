package ru.citeck.ecos.records2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.Objects;

public class RecordRef {

    public static final RecordRef EMPTY = new RecordRef();

    public static final String SOURCE_DELIMITER = "@";
    public static final String APP_NAME_DELIMITER = "/";
    private static final String DEFAULT_SOURCE_ID = "";

    private final String appName;
    private final String sourceId;
    private final String id;

    private RecordRef() {
        appName = "";
        sourceId = "";
        id = "";
    }

    /**
     * @deprecated use RecordRef.create instead
     */
    @Deprecated
    public RecordRef(String sourceId, String id) {
        this.sourceId = sourceId;
        this.id = id != null ? id : "";
        this.appName = "";
    }

    /**
     * @deprecated use RecordRef.create instead
     */
    @Deprecated
    public RecordRef(String sourceId, RecordRef id) {
        this.sourceId = sourceId;
        this.id = id.toString();
        this.appName = "";
    }

    private RecordRef(String appName, String sourceId, String id) {
        this.id = StringUtils.isNotBlank(id) ? id : "";
        this.appName = StringUtils.isNotBlank(appName) ? appName : "";
        this.sourceId = StringUtils.isNotBlank(sourceId) ? sourceId : "";
    }

    /**
     * @deprecated use RecordRef.valueOf instead
     */
    @Deprecated
    @JsonIgnore
    public RecordRef(String id) {

        int sourceDelimIdx = id.indexOf(SOURCE_DELIMITER);

        if (sourceDelimIdx != -1) {

            this.id = id.substring(sourceDelimIdx + 1);

            String source = id.substring(0, sourceDelimIdx);
            int appNameDelimIdx = source.indexOf(APP_NAME_DELIMITER);
            if (appNameDelimIdx > -1) {
                appName = source.substring(0, appNameDelimIdx);
                sourceId = source.substring(appNameDelimIdx + 1);
            } else {
                appName = "";
                sourceId = source;
            }
        } else {
            this.sourceId = DEFAULT_SOURCE_ID;
            this.id = id;
            this.appName = "";
        }
    }

    public static RecordRef create(String sourceId, String id) {
        if (StringUtils.isBlank(sourceId) && StringUtils.isBlank(id)) {
            return EMPTY;
        }
        return new RecordRef(sourceId, id);
    }

    public static RecordRef create(String sourceId, RecordRef id) {
        if (StringUtils.isBlank(sourceId) && id == EMPTY) {
            return EMPTY;
        }
        return new RecordRef(sourceId, id);
    }

    public static RecordRef create(String appName, String sourceId, String id) {
        if (StringUtils.isBlank(appName) && StringUtils.isBlank(sourceId) && StringUtils.isBlank(id)) {
            return EMPTY;
        }
        return new RecordRef(appName, sourceId, id);
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

    public String getAppName() {
        return appName;
    }

    public boolean isRemote() {
        return !appName.isEmpty();
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
        return Objects.equals(appName, that.appName)
            && Objects.equals(sourceId, that.sourceId)
            && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(sourceId);
        result = 31 * result + Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(appName);
        return result;
    }

    @JsonValue
    @Override
    public String toString() {
        if (this == EMPTY) {
            return "";
        }
        if (appName.length() > 0) {
            return appName + APP_NAME_DELIMITER + sourceId + SOURCE_DELIMITER + id;
        }
        if (sourceId.length() > 0) {
            return sourceId + SOURCE_DELIMITER + id;
        }
        return id;
    }
}
