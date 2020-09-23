package ru.citeck.ecos.records2.source.common;

import lombok.Data;
import ru.citeck.ecos.commons.utils.ReflectUtils;
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;

import java.util.List;

@Data
public class ParameterizedAttsMixin implements AttributesMixin<Object, Object> {

    private final AttributesMixin<Object, Object> impl;
    private final Class<?> reqMetaType;
    private final Class<?> resMetaType;

    @SuppressWarnings("unchecked")
    public ParameterizedAttsMixin(AttributesMixin<?, ?> impl) {

        this.impl = (AttributesMixin<Object, Object>) impl;
        List<Class<?>> genericArgs = ReflectUtils.getGenericArgs(impl.getClass(), AttributesMixin.class);

        if (genericArgs.size() != 2) {
            throw new IllegalArgumentException("Incorrect attributes mixin: [" + impl.getClass() + "] " + impl);
        }

        reqMetaType = genericArgs.get(0);
        resMetaType = genericArgs.get(1);
    }

    @Override
    public List<String> getAttributesList() {
        return impl.getAttributesList();
    }

    @Override
    public Object getAttribute(String attribute, Object meta) throws Exception {
        return impl.getAttribute(attribute, meta);
    }

    @Override
    public MetaEdge getEdge(String attribute, Object meta, UncheckedSupplier<MetaEdge> base) throws Exception {
        return impl.getEdge(attribute, meta, base);
    }

    @Override
    public Object getMetaToRequest() {
        return impl.getMetaToRequest();
    }
}
