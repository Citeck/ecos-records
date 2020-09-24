package ru.citeck.ecos.records3.graphql.meta.value;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;

public class MetaValueDelegate implements MetaValue {

    @Getter
    private final MetaValue impl;

    public MetaValueDelegate(MetaValue impl) {
        this.impl = impl == null ? EmptyValue.INSTANCE : impl;
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
    public String getId() throws Exception {
        return impl.getId();
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
    public MetaEdge getEdge(@NotNull String name) throws Exception {
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
    public RecordRef getRecordType() throws Exception {
        return impl.getRecordType();
    }
}
