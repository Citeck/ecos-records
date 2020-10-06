package ru.citeck.ecos.records3.record.operation.meta.value;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.SimpleAttEdge;

import java.util.Collections;

/**
 * Attribute value. Used to get attributes by schema.
 *
 * @author Pavel Simonov
 */
public interface AttValue {

    /**
     * Value identifier. Can be RecordRef or String
     */
    default Object getId() {
        return null;
    }

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
     * Get value attribute.
     */
    default Object getAtt(@NotNull String name) throws Exception {
        return Collections.emptyList();
    }

    default AttEdge getEdge(@NotNull String name) throws Exception {
        return new SimpleAttEdge(name, this);
    }

    default boolean has(@NotNull String name) throws Exception {
        return false;
    }

    default Double getDouble() throws Exception {
        String str = getString();
        return str != null ? Double.parseDouble(str) : null;
    }

    default Boolean getBool() throws Exception {
        return null;
    }

    default Object getJson() throws Exception {
        return getString();
    }

    default Object getAs(@NotNull String type) throws Exception {
        return null;
    }

    default RecordRef getTypeRef() throws Exception {
        return RecordRef.EMPTY;
    }
}
