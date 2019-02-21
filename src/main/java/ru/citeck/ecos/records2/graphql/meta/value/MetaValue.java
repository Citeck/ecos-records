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
    String getString();

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

    default boolean has(String name) throws Exception {
        return false;
    }

    default Double getDouble() {
        return Double.parseDouble(getString());
    }

    default Boolean getBool() {
        return Boolean.parseBoolean(getString());
    }

    default Object getJson() {
        return getString();
    }

    default MetaValue getAs(String type) {
        return null;
    }
}
