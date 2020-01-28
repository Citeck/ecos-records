package ru.citeck.ecos.records2.graphql.meta.value;

import ru.citeck.ecos.records2.RecordConstants;

public class EmptyValue implements MetaValue {

    public static EmptyValue INSTANCE = new EmptyValue();

    private EmptyValue() {
    }

    @Override
    public Object getAttribute(String name, MetaField field) {
        if (RecordConstants.ATT_NOT_EXISTS.equals(name)) {
            return true;
        }
        return null;
    }
}
