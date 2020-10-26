package ru.citeck.ecos.records3.record.op.atts.service.value.impl;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

public class RecordAttValue implements AttValue {

    private final RecordAtts atts;

    public RecordAttValue(RecordAtts atts) {
        this.atts = atts;
    }

    @Override
    public String getId() {
        return atts.getId().toString();
    }

    @Override
    public String asText() {
        return atts.getId().toString();
    }

    @Override
    public Object getAtt(@NotNull String name) {
        return atts.get(name);
    }
}
