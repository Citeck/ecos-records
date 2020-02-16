package ru.citeck.ecos.records2;

import ecos.com.fasterxml.jackson210.annotation.JsonCreator;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * Unique identifier of a record.
 */
public class RecordRef implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final RecordRef EMPTY = new RecordRef();

    public static final String SOURCE_DELIMITER = "@";
    public static final String APP_NAME_DELIMITER = "/";

    private final String appName;
    private final String sourceId;
    private final String id;

    private RecordRef() {
        appName = "";
        sourceId = "";
        id = "";
    }

    private RecordRef(String appName, String sourceId, String id) {
        this.id = StringUtils.isNotBlank(id) ? id : "";
        this.appName = StringUtils.isNotBlank(appName) ? appName : "";
        this.sourceId = StringUtils.isNotBlank(sourceId) ? sourceId : "";
    }

    public static RecordRef create(String sourceId, RecordRef id) {
        return create(sourceId, id.toString());
    }

    public static RecordRef create(String sourceId, String id) {
        return create("", sourceId, id);
    }

    public static RecordRef create(String appName, String sourceId, String id) {
        if (StringUtils.isBlank(appName) && StringUtils.isBlank(sourceId) && isBlankId(id)) {
            return EMPTY;
        }
        return new RecordRef(
                appName != null ? appName : "",
                sourceId != null ? sourceId : "",
                isBlankId(id) ? "" : id
        );
    }

    public RecordRef addAppName(String appName) {
        String current = toString();
        if (current.contains("@")) {
            return valueOf(appName + "/" + current);
        } else {
            return valueOf(appName + "/@" + current);
        }
    }

    public RecordRef withDefaultAppName(String appName) {
        if (StringUtils.isBlank(appName)) {
            return this;
        }
        return !this.appName.isEmpty() ? this : addAppName(appName);
    }

    public RecordRef removeAppName() {
        if (appName.isEmpty()) {
            return this;
        }
        return create(sourceId, id);
    }

    @JsonCreator
    @com.fasterxml.jackson.annotation.JsonCreator
    public static RecordRef valueOf(String recordRefStr) {

        if (isBlankId(recordRefStr)) {
            return EMPTY;
        }

        String id;
        String appName;
        String sourceId;

        int sourceDelimIdx = recordRefStr.indexOf(SOURCE_DELIMITER);

        if (sourceDelimIdx != -1) {

            id = recordRefStr.substring(sourceDelimIdx + 1);

            String source = recordRefStr.substring(0, sourceDelimIdx);
            int appNameDelimIdx = source.indexOf(APP_NAME_DELIMITER);
            if (appNameDelimIdx > -1) {
                appName = source.substring(0, appNameDelimIdx);
                sourceId = source.substring(appNameDelimIdx + 1);
            } else {
                appName = "";
                sourceId = source;
            }
        } else {
            id = recordRefStr;
            sourceId = "";
            appName = "";
        }

        return create(appName, sourceId, id);
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

    private static boolean isBlankId(String id) {
        return StringUtils.isBlank(id) || StringUtils.containsOnly(id, '@');
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
    @com.fasterxml.jackson.annotation.JsonValue
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
