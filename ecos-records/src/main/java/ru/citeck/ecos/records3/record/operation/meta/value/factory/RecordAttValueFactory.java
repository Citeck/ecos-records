package ru.citeck.ecos.records3.record.operation.meta.value.factory;

import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.RecordMetaValue;

import java.util.Collections;
import java.util.List;

public class RecordAttValueFactory implements AttValueFactory<RecordAtts> {

    @Override
    public AttValue getValue(RecordAtts value) {
        return new RecordMetaValue(value);
    }

    @Override
    public List<Class<? extends RecordAtts>> getValueTypes() {
        return Collections.singletonList(RecordAtts.class);
    }
}
