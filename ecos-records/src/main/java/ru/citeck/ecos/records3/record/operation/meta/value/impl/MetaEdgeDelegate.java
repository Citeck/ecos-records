package ru.citeck.ecos.records3.record.operation.meta.value.impl;

import lombok.Getter;
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier;
import ru.citeck.ecos.records3.record.operation.meta.value.AttEdge;

import java.util.List;

public class MetaEdgeDelegate implements AttEdge {

    @Getter
    private final AttEdge impl;
    private final UncheckedSupplier<Object> getValue;

    public MetaEdgeDelegate(AttEdge impl) {
        this.impl = impl;
        this.getValue = null;
    }

    public MetaEdgeDelegate(AttEdge impl, UncheckedSupplier<Object> getValue) {
        this.impl = impl;
        this.getValue = getValue;
    }

    @Override
    public boolean canBeRead() throws Exception {
        return impl.isProtected();
    }

    @Override
    public boolean isProtected() throws Exception {
        return impl.isProtected();
    }

    @Override
    public boolean isMultiple() throws Exception {
        return impl.isMultiple();
    }

    @Override
    public boolean isAssociation() throws Exception {
        return impl.isAssociation();
    }

    @Override
    public boolean isSearchable() throws Exception {
        return impl.isSearchable();
    }

    @Override
    public List<?> getOptions() throws Exception {
        return impl.getOptions();
    }

    @Override
    public List<?> getDistinct() throws Exception {
        return impl.getDistinct();
    }

    @Override
    public List<CreateVariant> getCreateVariants() throws Exception {
        return impl.getCreateVariants();
    }

    @Override
    public Class<?> getJavaClass() throws Exception {
        return impl.getJavaClass();
    }

    @Override
    public String getEditorKey() throws Exception {
        return impl.getEditorKey();
    }

    @Override
    public String getType() throws Exception {
        return impl.getType();
    }

    @Override
    public String getTitle() throws Exception {
        return impl.getTitle();
    }

    @Override
    public String getDescription() throws Exception {
        return impl.getDescription();
    }

    @Override
    public String getName() throws Exception {
        return impl.getName();
    }

    @Override
    public Object getValue() throws Exception {
        if (getValue != null) {
            return getValue.get();
        }
        return impl.getValue();
    }
}
