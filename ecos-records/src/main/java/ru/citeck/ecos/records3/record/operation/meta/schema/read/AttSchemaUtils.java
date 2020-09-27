package ru.citeck.ecos.records3.record.operation.meta.schema.read;

import ecos.com.fasterxml.jackson210.databind.JavaType;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.databind.type.TypeFactory;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;

import java.util.ArrayList;
import java.util.List;

public class AttSchemaUtils {

    private static final TypeFactory typeFactory = new ObjectMapper().getTypeFactory();

    public static JavaType getFlatAttType(SchemaRootAtt att) {
        return getFlatAttType(att.getAttribute());
    }

    public static JavaType getFlatAttType(SchemaAtt att) {
        return getFlatAttType(att, att.isMultiple());
    }

    private static JavaType getFlatAttType(SchemaAtt att, boolean multiple) {

        if (multiple) {
            return typeFactory.constructCollectionType(ArrayList.class, getFlatAttType(att, false));
        }

        String name = att.getName();

        if (name.charAt(0) == '.') {
            switch (name) {
                case ".json":
                    return typeFactory.constructType(JsonNode.class);
                case ".bool":
                    return typeFactory.constructType(Boolean.class);
                case ".num":
                    return typeFactory.constructType(Double.class);
                default:
                    return typeFactory.constructType(String.class);
            }
        }

        List<SchemaAtt> inner = att.getInner();

        if (inner.size() == 0) {
            throw new AttReadException(att.getAlias(), att.getName(),
                "Attribute is not a scalar and doesn't have inner attributes");
        } else if (inner.size() == 1) {
            return getFlatAttType(inner.get(0));
        } else {
            return typeFactory.constructType(ObjectData.class);
        }
    }
}
