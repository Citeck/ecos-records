package ru.citeck.ecos.records3.record.op.atts.service.value.impl;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

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
    public String asText() throws Exception {
        return impl.asText();
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
    public Double asDouble() throws Exception {
        return impl.asDouble();
    }

    @Override
    public Boolean asBool() throws Exception {
        return impl.asBool();
    }

    @Override
    public Object asJson() throws Exception {
        return impl.asJson();
    }

    @Override
    public Object as(@NotNull String type) throws Exception {
        return impl.as(type);
    }

    @Override
    public RecordRef getTypeRef() throws Exception {
        return impl.getTypeRef();
    }
}
