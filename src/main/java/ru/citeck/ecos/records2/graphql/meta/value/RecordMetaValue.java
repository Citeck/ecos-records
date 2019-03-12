package ru.citeck.ecos.records2.graphql.meta.value;

import ru.citeck.ecos.records2.RecordMeta;

public class RecordMetaValue implements MetaValue {

    private final RecordMeta meta;

    public RecordMetaValue(RecordMeta meta) {
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
    public Object getAttribute(String name, MetaField field) {
        return meta.get(name);
    }
}
