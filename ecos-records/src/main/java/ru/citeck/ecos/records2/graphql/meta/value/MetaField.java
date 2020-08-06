package ru.citeck.ecos.records2.graphql.meta.value;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;

import java.util.List;
import java.util.Map;

public interface MetaField {

    @NotNull
    default MetaField getParent() {
        return EmptyMetaField.INSTANCE;
    }

    @NotNull
    default String getKey() {
        String key = getAlias();
        if (StringUtils.isBlank(key)) {
            key = getName();
        }
        return key;
    }

    String getInnerSchema();

    String getAlias();

    String getName();

    @NotNull
    Map<String, MetaField> getSubFields();

    String getAttributeSchema(String field);

    List<String> getInnerAttributes();

    Map<String, String> getInnerAttributesMap();
}
