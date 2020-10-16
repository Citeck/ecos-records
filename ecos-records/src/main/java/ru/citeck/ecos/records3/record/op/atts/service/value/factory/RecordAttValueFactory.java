package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.RecordAttValue;

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
