package ru.citeck.ecos.records3.record.op.atts.value.factory;

import ru.citeck.ecos.records3.record.op.atts.RecordAtts;
import ru.citeck.ecos.records3.record.op.atts.value.AttValue;
import ru.citeck.ecos.records3.record.op.atts.value.impl.RecordAttValue;

import java.util.Collections;
import java.util.List;

public class RecordAttValueFactory implements AttValueFactory<RecordAtts> {

    @Override
    public AttValue getValue(RecordAtts value) {
        return new RecordAttValue(value);
    }

    @Override
    public List<Class<? extends RecordAtts>> getValueTypes() {
        return Collections.singletonList(RecordAtts.class);
    }
}
