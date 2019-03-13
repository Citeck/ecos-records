package ru.citeck.ecos.records2.graphql.meta.value;

import ru.citeck.ecos.records2.graphql.GqlContext;
import ru.citeck.ecos.records2.graphql.types.MetaValueTypeDef;

import java.util.Collections;

/**
 * Metadata value. Used to get attributes by schema
 *
 * @author Pavel Simonov
 * @see MetaValueTypeDef
 */
public interface MetaValue {

    /**
     * Initialize value with context before execute other methods
     */
    default <T extends GqlContext> void init(T context) {
    }

    /**
     * String representation
     */
    default String getString() {
        return toString();
    }

    default String getDisplayName() {
        return getString();
    }

    /**
     * Value identifier
     */
    default String getId() {
        return null;
    }

    /**
     * Get value attribute
     */
    default Object getAttribute(String name) throws Exception {
        return Collections.emptyList();
    }

    /**
     * Get value attribute
     */
    default Object getAttribute(String name, MetaField field) throws Exception {
        return getAttribute(name);
    }

    default MetaEdge getEdge(String name) {
        return new SimpleMetaEdge(name, this);
    }

    default MetaEdge getEdge(String name, MetaField field) {
        return getEdge(name);
    }

    default boolean has(String name) throws Exception {
        return false;
    }

    default Double getDouble() {
        String str = getString();
        return str != null ? Double.parseDouble(str) : null;
    }

    default Boolean getBool() {
        String str = getString();
        return str != null ? Boolean.parseBoolean(getString()) : null;
    }

    default Object getJson() {
        return getString();
    }

    default MetaValue getAs(String type) {
        return null;
    }
}
