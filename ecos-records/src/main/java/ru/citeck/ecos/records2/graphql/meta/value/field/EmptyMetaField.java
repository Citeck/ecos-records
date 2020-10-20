package ru.citeck.ecos.records2.graphql.meta.value.field;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EmptyMetaField implements MetaField {

    public static final MetaField INSTANCE = new EmptyMetaField();

    private EmptyMetaField() {
    }

    @Override
    public String getInnerSchema() {
        return "";
    }

    @Override
    public String getAlias() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getAttributeSchema(String field) {
        return "";
    }

    @Override
    public List<String> getInnerAttributes() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getInnerAttributesMap() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getInnerAttributesMap(boolean withAliases) {
        return Collections.emptyMap();
    }
}
