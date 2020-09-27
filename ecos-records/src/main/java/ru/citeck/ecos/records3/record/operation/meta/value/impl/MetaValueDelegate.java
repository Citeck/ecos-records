package ru.citeck.ecos.records3.record.operation.meta.value.impl;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.meta.value.AttEdge;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

public class MetaValueDelegate implements AttValue {

    @Getter
    private final AttValue impl;

    public MetaValueDelegate(AttValue impl) {
        this.impl = impl == null ? EmptyValue.INSTANCE : impl;
    }

    @Override
    public RecordRef getRef() throws Exception {
        return impl.getRef();
    }

    @Override
    public String getString() throws Exception {
        return impl.getString();
    }

    @Override
    public String getDisplayName() throws Exception {
        return impl.getDisplayName();
    }

    @Override
    public String getLocalId() throws Exception {
        return impl.getLocalId();
    }

    @Override
    public Object getAttribute(@NotNull String name) throws Exception {
        return impl.getAttribute(name);
    }

    @Override
    public AttEdge getEdge(@NotNull String name) throws Exception {
        return impl.getEdge(name);
    }

    @Override
    public boolean has(@NotNull String name) throws Exception {
        return impl.has(name);
    }

    @Override
    public Double getDouble() throws Exception {
        return impl.getDouble();
    }

    @Override
    public Boolean getBool() throws Exception {
        return impl.getBool();
    }

    @Override
    public Object getJson() throws Exception {
        return impl.getJson();
    }

    @Override
    public Object getAs(@NotNull String type) throws Exception {
        return impl.getAs(type);
    }

    @Override
    public RecordRef getTypeRef() throws Exception {
        return impl.getTypeRef();
    }
}
