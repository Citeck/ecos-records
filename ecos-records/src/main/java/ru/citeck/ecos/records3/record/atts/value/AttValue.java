package ru.citeck.ecos.records3.record.atts.value;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.future.RecFuture;
import ru.citeck.ecos.records3.record.atts.value.impl.SimpleAttEdge;
import ru.citeck.ecos.records3.utils.AttUtils;

/**
 * Attribute value. Used to get attributes by schema.
 *
 * @author Pavel Simonov
 */
public interface AttValue {

    /**
     * Method to initialize AttValue before it will be resolved by AttSchemaResolver
     * This method may be used to pre evaluate inner attributes using AttContext
     */
    @Nullable
    default RecFuture<?> init() throws Exception {
        return null;
    }

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
    default Object getAs(@NotNull String type) throws Exception {
        return null;
    }

    @Nullable
    default Double asDouble() throws Exception {
        String text = asText();
        if (text == null || StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            AttUtils.INSTANCE.logError(
                this,
                "Number format exception: " + e.getMessage()
            );
            return null;
        }
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

    @Nullable
    default Object asRaw() throws Exception {
        return asText();
    }

    @Nullable
    default Object asBin() throws Exception {
        return asText();
    }

    default boolean has(@NotNull String name) throws Exception {
        return false;
    }

    @Nullable
    default Object getAtt(@NotNull String name) throws Exception {
        return null;
    }

    @Nullable
    default AttEdge getEdge(@NotNull String name) throws Exception {
        return new SimpleAttEdge(name, this);
    }

    @Nullable
    default Object getType() throws Exception {
        return null;
    }
}
