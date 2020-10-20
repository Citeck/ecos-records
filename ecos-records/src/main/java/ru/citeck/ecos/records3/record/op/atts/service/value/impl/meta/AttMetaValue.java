package ru.citeck.ecos.records3.record.op.atts.service.value.impl.meta;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

/**
 * Adapter MetaValue -> AttValue
 */
@RequiredArgsConstructor
public class AttMetaValue implements AttValue {

    private final MetaValue metaValue;

    @Override
    public Object getId() {
        return metaValue.getId();
    }

    @Override
    public String getString() {
        return metaValue.getString();
    }

    @Override
    public String getDisplayName() {
        return metaValue.getDisplayName();
    }

    @Override
    public Object getAtt(@NotNull String name) throws Exception {
        return metaValue.getAttribute(name, EmptyMetaField.INSTANCE);
    }

    @Override
    public AttEdge getEdge(@NotNull String name) {
        return new AttMetaEdge(metaValue.getEdge(name, EmptyMetaField.INSTANCE));
    }

    @Override
    public boolean has(@NotNull String name) throws Exception {
        return metaValue.has(name);
    }

    @Override
    public Double getDouble() {
        return metaValue.getDouble();
    }

    @Override
    public Boolean getBool() {
        return metaValue.getBool();
    }

    @Override
    public Object getJson() {
        return metaValue.getJson();
    }

    @Override
    public Object getAs(@NotNull String type) {
        return metaValue.getAs(type, EmptyMetaField.INSTANCE);
    }

    @Override
    public RecordRef getTypeRef() {
        return metaValue.getRecordType();
    }
}
