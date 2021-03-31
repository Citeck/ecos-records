package ru.citeck.ecos.records2.graphql.meta.value;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordRef;

import java.util.Collections;

/**
 * Metadata value. Used to get attributes by schema.
 *
 * @deprecated -> AttValue
 *
 * @author Pavel Simonov
 */
@Deprecated
public interface MetaValue {

    /**
     * Initialize value with context before execute other methods.
     */
    default <T extends QueryContext> void init(@NotNull T context, @NotNull MetaField field) {
    }

    /**
     * String representation.
     */
    default String getString() {
        return toString();
    }

    default String getDisplayName() {
        return getString();
    }

    /**
     * Value identifier.
     */
    default String getId() {
        return null;
    }

    default String getLocalId() {
        String id = getId();
        if (StringUtils.isNotBlank(id) && id.contains(RecordRef.SOURCE_DELIMITER)) {
            return RecordRef.valueOf(id).getId();
        }
        return id;
    }

    /**
     * Get value attribute.
     */
    default Object getAttribute(@NotNull String name, @NotNull MetaField field) throws Exception {
        return Collections.emptyList();
    }

    default MetaEdge getEdge(@NotNull String name, @NotNull MetaField field) {
        return new SimpleMetaEdge(name, this);
    }

    default boolean has(@NotNull String name) throws Exception {
        return false;
    }

    default Double getDouble() {
        String str = getString();
        if (StringUtils.isBlank(str)) {
            return null;
        }
        return Double.parseDouble(str);
    }

    default Boolean getBool() {
        return Boolean.parseBoolean(getString());
    }

    default Object getJson() {
        return getString();
    }

    default Object getAs(@NotNull String type, @NotNull MetaField field) {
        return getAs(type);
    }

    @Deprecated
    default Object getAs(@NotNull String type) {
        return null;
    }

    default RecordRef getRecordType() {
        return RecordRef.EMPTY;
    }
}
