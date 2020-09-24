package ru.citeck.ecos.records3.graphql.meta.value;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordConstants;

public class EmptyValue implements MetaValue {

    public static final EmptyValue INSTANCE = new EmptyValue();

    private EmptyValue() {
    }

    @Override
    public Object getAttribute(@NotNull String name) {
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
