package ru.citeck.ecos.records3.record.op.atts.value.impl;

import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records3.record.op.atts.value.AttValue;

public class AttIdValue implements AttValue {

    private final String id;

    public AttIdValue(Object id) {
        MandatoryParam.check("id", id);
        this.id = id.toString();
    }

    @Override
    public String getString() {
        return id;
    }

    @Override
    public String getId() {
        return id;
    }
}
