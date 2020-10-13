package ru.citeck.ecos.records3.record.op.atts.value.impl;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.atts.value.AttEdge;
import ru.citeck.ecos.records3.record.op.atts.value.AttValue;

public class AttValueDelegate implements AttValue {

    @Getter
    private final AttValue impl;

    public AttValueDelegate(AttValue impl) {
        this.impl = impl == null ? EmptyAttValue.INSTANCE : impl;
    }

    @Override
    public Object getId() {
        return impl.getId();
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
