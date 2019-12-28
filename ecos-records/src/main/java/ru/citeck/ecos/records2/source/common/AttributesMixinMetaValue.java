package ru.citeck.ecos.records2.source.common;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValueDelegate;
import ru.citeck.ecos.records2.meta.RecordsMetaService;

import java.util.List;
import java.util.Map;

public class AttributesMixinMetaValue extends MetaValueDelegate {

    private List<AttributesMixin<Object>> mixins;
    private RecordsMetaService recordsMetaService;
    private Map<Class<?>, Object> metaCache;

    public AttributesMixinMetaValue(MetaValue impl,
                                    RecordsMetaService recordsMetaService,
                                    List<AttributesMixin<?>> mixins,
                                    Map<Class<?>, Object> metaCache) {
        super(impl);
        @SuppressWarnings("unchecked")
        List<AttributesMixin<Object>> typedMixins = (List<AttributesMixin<Object>>) (List<?>) mixins;
        this.mixins = typedMixins;
        this.metaCache = metaCache;
        this.recordsMetaService = recordsMetaService;
    }

    @Override
    public Object getAttribute(String name, MetaField field) throws Exception {

        for (AttributesMixin<Object> mixin : mixins) {
            if (mixin.hasAttribute(name)) {

                Class<?> metaType = mixin.getRequiredMetaType();

                if (metaType == null) {
                    return mixin.getAttribute(name, null, field);
                }

                Object requiredMeta = metaCache.computeIfAbsent(metaType, t ->
                    recordsMetaService.getMeta(this, t));

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
