package ru.citeck.ecos.records3.record.operation.meta.schema.read;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;

import java.util.List;

public class AttSchemaUtils {

    @Nullable
    public static Class<?> getFlatScalarType(@NotNull SchemaAtt att) {

        String name = att.getName();

        if (att.isScalar()) {
            switch (name) {
                case "json":
                    return JsonNode.class;
                case "bool":
                    return Boolean.class;
                case "num":
                    return Double.class;
                default:
                    return String.class;
            }
        }

        List<SchemaAtt> inner = att.getInner();

        if (inner.size() == 0) {
            throw new AttReadException(att.getAlias(), att.getName(),
                "Attribute is not a scalar and doesn't have inner attributes");
        } else if (inner.size() == 1) {
            return getFlatScalarType(inner.get(0));
        }

        return null;
    }
}
