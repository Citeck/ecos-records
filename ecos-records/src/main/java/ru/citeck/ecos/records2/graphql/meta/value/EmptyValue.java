package ru.citeck.ecos.records2.graphql.meta.value;

import ru.citeck.ecos.records2.RecordConstants;

/**
 * @deprecated use EmptyAttValue
 */
@Deprecated
public class EmptyValue implements MetaValue {

    public static final EmptyValue INSTANCE = new EmptyValue();

    private EmptyValue() {
    }

    @Override
    public Object getAttribute(String name, MetaField field) {
        if (RecordConstants.ATT_NOT_EXISTS.equals(name)) {
            return true;
        }
        return null;
    }

    @Override
    public String getString() {
        return null;
    }
}
