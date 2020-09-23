package ru.citeck.ecos.records2.graphql.meta.value;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordRef;

import java.util.Collections;

/**
 * Metadata value. Used to get attributes by schema.
 *
 * @author Pavel Simonov
 */
public interface MetaValue {

    /**
     * String representation.
     */
    default String getString() throws Exception {
        return toString();
    }

    default String getDisplayName() throws Exception {
        return getString();
    }

    /**
     * Value identifier.
     */
    default String getId() throws Exception {
        return null;
    }

    default String getLocalId() throws Exception {
        String id = getId();
        if (StringUtils.isNotBlank(id) && id.contains(RecordRef.SOURCE_DELIMITER)) {
            return RecordRef.valueOf(id).getId();
        }
        return id;
    }

    /**
     * Get value attribute.
     */
    default Object getAttribute(@NotNull String name) throws Exception {
        return Collections.emptyList();
    }

    default MetaEdge getEdge(@NotNull String name) throws Exception {
        return new SimpleMetaEdge(name, this);
    }

    default boolean has(@NotNull String name) throws Exception {
        return false;
    }

    default Double getDouble() throws Exception {
        String str = getString();
        return str != null ? Double.parseDouble(str) : null;
    }

    default Boolean getBool() throws Exception {
        return Boolean.parseBoolean(getString());
    }

    default Object getJson() throws Exception {
        return getString();
    }

    default Object getAs(@NotNull String type) throws Exception {
        return null;
    }

    default RecordRef getRecordType() throws Exception {
        return RecordRef.EMPTY;
    }
}
