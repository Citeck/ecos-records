package ru.citeck.ecos.records3.record.op.atts.service.value;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.SimpleAttEdge;

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

    default String getDispName() throws Exception {
        return asText();
    }

    // ===== get as =====

    default Object as(@NotNull String type) throws Exception {
        return null;
    }

    /**
     * String representation.
     */
    default String asText() throws Exception {
        return toString();
    }

    default Double asDouble() throws Exception {
        String str = asText();
        return str != null ? Double.parseDouble(str) : null;
    }

    default Boolean asBool() throws Exception {
        String txt = asText();
        if (Boolean.TRUE.toString().equals(txt)) {
            return true;
        } else if (Boolean.FALSE.toString().equals(txt)) {
            return false;
        }
        return null;
    }

    default Object asJson() throws Exception {
        return asText();
    }

    // ===== /get as =====

    default RecordRef getTypeRef() throws Exception {
        return RecordRef.EMPTY;
    }

    default boolean has(@NotNull String name) throws Exception {
        return false;
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
}
