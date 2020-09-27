package ru.citeck.ecos.records3.record.operation.meta.value.impl;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

public class RecordMetaValue implements AttValue {

    private final RecordAtts meta;

    public RecordMetaValue(RecordAtts meta) {
        this.meta = meta;
    }

    @Override
    public String getId() {
        return meta.getId().toString();
    }

    @Override
    public String getString() {
        return meta.getId().toString();
    }

    @Override
    public Object getAttribute(@NotNull String name) {
        return meta.get(name);
    }
}
