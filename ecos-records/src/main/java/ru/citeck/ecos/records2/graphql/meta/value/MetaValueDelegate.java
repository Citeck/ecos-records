package ru.citeck.ecos.records2.graphql.meta.value;

import lombok.Getter;
import ru.citeck.ecos.records2.QueryContext;

public class MetaValueDelegate implements MetaValue {

    @Getter
    private final MetaValue impl;

    public MetaValueDelegate(MetaValue impl) {
        this.impl = impl;
    }

    @Override
    public <T extends QueryContext> void init(T context, MetaField field) {
        impl.init(context, field);
    }

    @Override
    public String getString() {
        return impl.getString();
    }

    @Override
    public String getDisplayName() {
        return impl.getDisplayName();
    }

    @Override
    public String getId() {
        return impl.getId();
    }

    @Override
    public Object getAttribute(String name, MetaField field) throws Exception {
        return impl.getAttribute(name, field);
    }

    @Override
    public MetaEdge getEdge(String name, MetaField field) {
        return impl.getEdge(name, field);
    }

    @Override
    public boolean has(String name) throws Exception {
        return impl.has(name);
    }

    @Override
    public Double getDouble() {
        return impl.getDouble();
    }

    @Override
    public Boolean getBool() {
        return impl.getBool();
    }

    @Override
    public Object getJson() {
        return impl.getJson();
    }

    @Override
    public Object getAs(String type) {
        return impl.getAs(type);
    }
}
