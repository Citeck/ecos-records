package ru.citeck.ecos.records2.graphql.meta.value;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.types.MetaValueTypeDef;

import java.util.Collections;

/**
 * Metadata value. Used to get attributes by schema.
 *
 * @author Pavel Simonov
 *
 * @see MetaValueTypeDef
 */
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
        return str != null ? Double.parseDouble(str) : null;
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
