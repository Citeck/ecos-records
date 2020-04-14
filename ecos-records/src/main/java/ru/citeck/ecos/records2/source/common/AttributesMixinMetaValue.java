package ru.citeck.ecos.records2.source.common;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValueDelegate;
import ru.citeck.ecos.records2.meta.RecordsMetaService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class AttributesMixinMetaValue extends MetaValueDelegate {

    private Map<String, ParameterizedAttsMixin> mixins;
    private RecordsMetaService recordsMetaService;
    private Map<Object, Object> metaCache;

    public AttributesMixinMetaValue(MetaValue impl,
                                    RecordsMetaService recordsMetaService,
                                    Map<String, ParameterizedAttsMixin> mixins,
                                    Map<Object, Object> metaCache) {
        super(impl);
        this.mixins = mixins;
        this.metaCache = metaCache;
        this.recordsMetaService = recordsMetaService;
    }

    private <R> R doWithMeta(ParameterizedAttsMixin mixin, UncheckedFunction<Object, R> action) throws Exception {

        if (mixin == null) {
            return null;
        }

        Class<?> resMetaType = mixin.getResMetaType();
        if (MetaValue.class.equals(resMetaType)) {
            return action.apply(this);
        } else if (RecordRef.class.equals(resMetaType)) {
            return action.apply(RecordRef.valueOf(getId()));
        }

        Object metaToRequest = mixin.getMetaToRequest();

        if (metaToRequest == null || resMetaType == null) {
            return action.apply(null);
        }

        if (metaToRequest instanceof Map) {
            metaToRequest = new HashMap<>((Map<?, ?>) metaToRequest);
        }

        Object requiredMeta = metaCache.computeIfAbsent(metaToRequest, reqMeta -> {

            if (reqMeta instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> attributes = (Map<String, String>) reqMeta;
                RecordMeta resMeta = recordsMetaService.getMeta(this, attributes);
                if (resMetaType.isAssignableFrom(RecordMeta.class)) {
                    return resMeta;
                }
                return recordsMetaService.instantiateMeta(resMetaType, resMeta);
            } else if (reqMeta instanceof Class) {
                return recordsMetaService.getMeta(this, (Class<?>) reqMeta);
            } else {
                return recordsMetaService.getMeta(this, reqMeta.getClass());
            }
        });

        return action.apply(requiredMeta);
    }

    @Override
    public Object getAttribute(String attribute, MetaField field) throws Exception {

        ParameterizedAttsMixin mixin = mixins.get(attribute);

        if (mixin == null) {
            return super.getAttribute(attribute, field);
        }
        return doWithMeta(mixin, meta -> {

            if (meta == this) {

                MetaValue implMeta = getImpl();

                return mixin.getAttribute(attribute, new MetaValueDelegate(this) {
                    @Override
                    public Object getAttribute(String name, MetaField field) throws Exception {
                        if (attribute.equals(name)) {
                            return implMeta.getAttribute(name, field);
                        }
                        return super.getAttribute(name, field);
                    }
                }, field);
            }

            return mixin.getAttribute(attribute, meta, field);
        });
    }

    @Override
    public MetaEdge getEdge(String attribute, MetaField field) {

        ParameterizedAttsMixin mixin = mixins.get(attribute);

        if (mixin == null) {
            return super.getEdge(attribute, field);
        }

        try {

            return doWithMeta(mixin, meta -> {

                MetaValue implMeta = getImpl();
                Supplier<MetaEdge> edgeSupplier = () -> implMeta.getEdge(attribute, field);

                if (meta == this) {

                    return mixin.getEdge(attribute, new MetaValueDelegate(this) {
                        @Override
                        public MetaEdge getEdge(String name, MetaField field) {
                            if (attribute.equals(name)) {
                                return implMeta.getEdge(name, field);
                            }
                            return super.getEdge(name, field);
                        }
                    }, edgeSupplier, field);
                }

                return mixin.getEdge(attribute, meta, edgeSupplier, field);
            });

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean has(String name) throws Exception {
        return mixins.containsKey(name) || super.has(name);
    }
}
