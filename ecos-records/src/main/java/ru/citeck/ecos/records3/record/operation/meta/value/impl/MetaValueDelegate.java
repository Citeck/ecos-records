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
    public String getDispName() throws Exception {
        return impl.getDispName();
    }

   @Override
    public Object getAtt(@NotNull String name) throws Exception {
        return impl.getAtt(name);
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
