package ru.citeck.ecos.records2.graphql.meta.value;

import ru.citeck.ecos.records2.utils.MandatoryParam;

public class MetaIdValue implements MetaValue {

    private String id;

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