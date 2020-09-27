package ru.citeck.ecos.records3.record.operation.meta.value.impl;

import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

public class MetaIdValue implements AttValue {

    private final String id;

    public MetaIdValue(Object id) {
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
