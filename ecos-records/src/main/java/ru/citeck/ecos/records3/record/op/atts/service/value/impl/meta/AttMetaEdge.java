package ru.citeck.ecos.records3.record.op.atts.service.value.impl.meta;

import lombok.RequiredArgsConstructor;
import ru.citeck.ecos.records2.graphql.meta.value.CreateVariant;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge;

import java.util.List;

/**
 * Adapter MetaEdge -> AttEdge
 */
@RequiredArgsConstructor
public class AttMetaEdge implements AttEdge {

    private final MetaEdge edge;

    @Override
    public boolean isProtected() {
        return edge.isProtected();
    }

    @Override
    public boolean isMultiple() {
        return edge.isMultiple();
    }

    @Override
    public boolean isAssociation() {
        return edge.isAssociation();
    }

    @Override
    public boolean isSearchable() {
        return edge.isSearchable();
    }

    @Override
    public boolean canBeRead() {
        return edge.canBeRead();
    }

    @Override
    public List<?> getOptions() {
        return edge.getOptions();
    }

    @Override
    public List<?> getDistinct() {
        return edge.getDistinct();
    }

    @Override
    public List<CreateVariant> getCreateVariants() {
        return edge.getCreateVariants();
    }

    @Override
    public Class<?> getJavaClass() {
        return edge.getJavaClass();
    }

    @Override
    public String getEditorKey() {
        return edge.getEditorKey();
    }

    @Override
    public String getType() {
        return edge.getType();
    }

    @Override
    public String getTitle() {
        return edge.getTitle();
    }

    @Override
    public String getDescription() {
        return edge.getDescription();
    }

    @Override
    public String getName() {
        return edge.getName();
    }

    @Override
    public Object getValue() throws Exception {
        return edge.getValue(EmptyMetaField.INSTANCE);
    }
}
