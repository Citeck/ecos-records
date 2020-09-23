package ru.citeck.ecos.records2.graphql.meta.value;

import org.jetbrains.annotations.NotNull;
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
    public Object getAttribute(@NotNull String name) {
        return meta.get(name);
    }
}
