package ru.citeck.ecos.records2.graphql.meta.value;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

/**
 * @deprecated -> AttValueDelegate
 */
@Deprecated
public class MetaValueDelegate implements MetaValue {

    @Getter
    private final MetaValue impl;

    public MetaValueDelegate(MetaValue impl) {
        this.impl = impl == null ? EmptyValue.INSTANCE : impl;
    }

    @Override
    public <T extends QueryContext> void init(@NotNull T context, @NotNull MetaField field) {
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
    public String getLocalId() {
        return impl.getLocalId();
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
    public Object getRaw() {
        return impl.getRaw();
    }

    @Override
    public Object getAs(String type) {
        return impl.getAs(type);
    }

    @Override
    public Object getAs(String type, MetaField field) {
        return impl.getAs(type, field);
    }

    @Override
    public EntityRef getRecordType() {
        return impl.getRecordType();
    }
}
