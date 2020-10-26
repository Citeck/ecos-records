package ru.citeck.ecos.records3.record.op.atts.service.value.impl;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

public class EmptyAttValue implements AttValue {

    public static final EmptyAttValue INSTANCE = new EmptyAttValue();

    private EmptyAttValue() {
    }

    @Override
    public Object getAtt(@NotNull String name) {
        if (RecordConstants.ATT_NOT_EXISTS.equals(name)) {
            return true;
        }
        return null;
    }

    @Override
    public String asText() {
        return null;
    }
}
