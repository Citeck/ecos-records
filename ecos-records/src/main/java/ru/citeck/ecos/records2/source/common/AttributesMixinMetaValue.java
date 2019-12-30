package ru.citeck.ecos.records2.source.common;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValueDelegate;
import ru.citeck.ecos.records2.meta.RecordsMetaService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributesMixinMetaValue extends MetaValueDelegate {

    private List<ParameterizedAttsMixin> mixins;
    private RecordsMetaService recordsMetaService;
    private Map<Object, Object> metaCache;

    public AttributesMixinMetaValue(MetaValue impl,
                                    RecordsMetaService recordsMetaService,
                                    List<ParameterizedAttsMixin> mixins,
                                    Map<Object, Object> metaCache) {
        super(impl);
        this.mixins = mixins;
        this.metaCache = metaCache;
        this.recordsMetaService = recordsMetaService;
    }

    @Override
    public Object getAttribute(String name, MetaField field) throws Exception {

        for (ParameterizedAttsMixin mixin : mixins) {
            if (mixin.hasAttribute(name)) {

                Object metaToRequest = mixin.getMetaToRequest();
                Class<?> resMetaType = mixin.getResMetaType();

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
        }
        return super.getAttribute(name, field);
    }

    @Override
    public boolean has(String name) throws Exception {
        return mixins.stream().anyMatch(m -> m.hasAttribute(name)) || super.has(name);
    }
}
