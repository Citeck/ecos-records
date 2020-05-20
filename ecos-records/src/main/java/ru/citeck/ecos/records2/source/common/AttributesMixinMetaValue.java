package ru.citeck.ecos.records2.source.common;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.ScriptUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValueDelegate;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.type.ComputedAttribute;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class AttributesMixinMetaValue extends MetaValueDelegate {

    private final RecordsMetaService recordsMetaService;

    private final Map<String, ParameterizedAttsMixin> mixins;
    private final Map<Object, Object> metaCache;

    private final Map<String, ComputedAttribute> computedAtts;

    public AttributesMixinMetaValue(MetaValue impl,
                                    RecordsMetaService recordsMetaService,
                                    Map<String, ComputedAttribute> computedAtts,
                                    Map<String, ParameterizedAttsMixin> mixins,
                                    Map<Object, Object> metaCache) {
        super(impl);
        this.mixins = mixins;
        this.metaCache = metaCache;
        this.computedAtts = computedAtts;
        this.recordsMetaService = recordsMetaService;
    }

    private <R, M> R doWithMeta(ParameterizedAttsMixin mixin,
                                UncheckedFunction<Object, R> action) throws Exception {

        return doWithMeta(mixin.getMetaToRequest(), mixin.getResMetaType(), action);
    }

    private <R> R doWithMeta(Object metaToRequest,
                             Class<?> resMetaType,
                             UncheckedFunction<Object, R> action) throws Exception {

        if (MetaValue.class.equals(resMetaType)) {
            return action.apply(this);
        } else if (RecordRef.class.equals(resMetaType)) {
            return action.apply(RecordRef.valueOf(getId()));
        }

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
            ComputedAttribute computedAtt = computedAtts.get(attribute);
            if (computedAtt != null) {
                return computeAttribute(computedAtt);
            } else {
                return super.getAttribute(attribute, field);
            }
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

    private Object computeAttribute(ComputedAttribute computedAtt) throws Exception {

        return this.doWithMeta(computedAtt.getModel(), ObjectData.class, meta -> {

            ObjectData metaObj = ObjectData.create(meta);
            String type = computedAtt.getType();

            if (StringUtils.isBlank(type)) {

                if (metaObj.has("<")) {
                    return metaObj.get("<");
                } else {
                    return metaObj;
                }

            } else if ("script".equals(type)) {

                String script = computedAtt.getConfig().get("script").asText();

                if (StringUtils.isBlank(script)) {
                    log.warn("Script is blank. Att: " + computedAtt);
                    return null;
                }

                return ScriptUtils.eval(script, Collections.singletonMap("M", metaObj));

            } else {
                log.error("Computed attribute type is not supported: '" + type + "'. att: " + computedAtt);
            }
            return null;
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
            ExceptionUtils.throwException(e);
            return null;
        }
    }

    @Override
    public boolean has(String name) throws Exception {
        return mixins.containsKey(name) || super.has(name);
    }
}
