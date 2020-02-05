package ru.citeck.ecos.records2.source.common;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValueDelegate;
import ru.citeck.ecos.records2.meta.RecordsMetaService;

import java.util.HashMap;
import java.util.Map;

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

    @Override
    public Object getAttribute(String name, MetaField field) throws Exception {

        ParameterizedAttsMixin mixin = mixins.get(name);

        if (mixin == null) {
            return super.getAttribute(name, field);
        }

        Class<?> resMetaType = mixin.getResMetaType();
        if (MetaValue.class.equals(resMetaType)) {
            return mixin.getAttribute(name, this, field);
        } else if (RecordRef.class.equals(resMetaType)) {
            return mixin.getAttribute(name, RecordRef.valueOf(getId()), field);
        }

        Object metaToRequest = mixin.getMetaToRequest();

        if (metaToRequest == null || resMetaType == null) {
            return mixin.getAttribute(name, null, field);
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

        return mixin.getAttribute(name, requiredMeta, field);
    }

    @Override
    public boolean has(String name) throws Exception {
        return mixins.containsKey(name) || super.has(name);
    }
}
