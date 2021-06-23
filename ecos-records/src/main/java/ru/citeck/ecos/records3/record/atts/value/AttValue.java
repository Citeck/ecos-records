package ru.citeck.ecos.records3.record.atts.value;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.atts.value.impl.SimpleAttEdge;

/**
 * Attribute value. Used to get attributes by schema.
 *
 * @author Pavel Simonov
 */
public interface AttValue {

    @Nullable
    default Object getId() throws Exception {
        return null;
    }

    @Nullable
    default Object getDisplayName() throws Exception {
        return asText();
    }

    @Nullable
    default String asText() throws Exception {
        return toString();
    }

    @Nullable
    default Object getAs(String type) throws Exception {
        return null;
    }

    @Nullable
    default Double asDouble() throws Exception {
        String text = asText();
        if (text == null || StringUtils.isBlank(text)) {
            return null;
        }
        return Double.parseDouble(text);
    }

    @Nullable
    default Boolean asBoolean() throws Exception {
        String text = asText();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        } else if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }

    @Nullable
    default Object asJson() throws Exception {
        return Json.getMapper().read(asText());
    }

    default boolean has(String name) throws Exception {
        return false;
    }

    @Nullable
    default Object getAtt(String name) throws Exception {
        return null;
    }

    @Nullable
    default AttEdge getEdge(String name) throws Exception {
        return new SimpleAttEdge(name, this);
    }

    @NotNull
    default RecordRef getType() throws Exception {
        return RecordRef.EMPTY;
    }
}
