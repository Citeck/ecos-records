package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.RecordMetaValue;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;

import java.util.Collections;
import java.util.List;

public class RecordMetaValueFactory implements MetaValueFactory<RecordMeta> {

    @Override
    public MetaValue getValue(RecordMeta value) {
        return new RecordMetaValue(value);
    }

    @Override
    public List<Class<? extends RecordMeta>> getValueTypes() {
        return Collections.singletonList(RecordMeta.class);
    }
}
