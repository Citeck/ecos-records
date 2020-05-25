package ru.citeck.ecos.records2.source.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.*;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValueDelegate;
import ru.citeck.ecos.records2.graphql.meta.value.factory.RecordRefValueFactory;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.type.ComputedAttribute;
import ru.citeck.ecos.records2.type.RecordTypeService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Slf4j
@SuppressFBWarnings({ "NP_BOOLEAN_RETURN_NULL" })
public class AttributesMixinMetaValue extends MetaValueDelegate {

    private final RecordsMetaService recordsMetaService;
    private final RecordTypeService recordTypeService;

    private final Map<String, ParameterizedAttsMixin> mixins;
    private final Map<Object, Object> metaCache;

    private Map<String, ComputedAttribute> computedAtts = Collections.emptyMap();

    public AttributesMixinMetaValue(MetaValue impl,
                                    RecordsMetaService recordsMetaService,
                                    RecordTypeService recordTypeService,
                                    Map<String, ParameterizedAttsMixin> mixins,
                                    Map<Object, Object> metaCache) {
        super(impl);
        this.mixins = mixins;
        this.metaCache = metaCache;
        this.recordTypeService = recordTypeService;
        this.recordsMetaService = recordsMetaService;
    }

    @Override
    public <T extends QueryContext> void init(T context, MetaField field) {
        super.init(context, field);
        computedAtts = recordTypeService.getComputedAttributes(getType());
    }

    private RecordRef getType() {
        Object typeRef = RecordRef.EMPTY;
        try {
            typeRef = super.getAttribute(RecordConstants.ATT_ECOS_TYPE, EmptyMetaField.INSTANCE);
        } catch (Exception e) {
            try {
                log.error("Type can't be received for " + getId());
            } catch (Exception ex) {
                log.error("getId failed");
            }
            ExceptionUtils.throwException(e);
        }
        if (typeRef instanceof Iterable) {
            typeRef = IterUtils.first((Iterable<?>) typeRef).orElse(null);
        }
        if (typeRef instanceof RecordRefValueFactory.RecordRefValue) {
            typeRef = ((RecordRefValueFactory.RecordRefValue) typeRef).getRef();
        }
        return typeRef instanceof RecordRef ? (RecordRef) typeRef : RecordRef.EMPTY;
    }

    @Override
    public String getDisplayName() {
        return getAttributeImpl(".disp", String.class, super::getDisplayName);
    }

    @Override
    public String getString() {
        return getAttributeImpl(".str", String.class, super::getString);
    }

    @Override
    public Double getDouble() {
        return getAttributeImpl(".num", Double.class, super::getDouble);
    }

    @Override
    public Boolean getBool() {
        return getAttributeImpl(".bool", Boolean.class, super::getBool);
    }

    @Override
    public Object getJson() {
        return getAttributeImpl(".json", EmptyMetaField.INSTANCE, super::getJson);
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
    public Object getAttribute(String attribute, MetaField field) {
        return getAttributeImpl(attribute, field, () -> super.getAttribute(attribute, field));
    }

    private <T> T getAttributeImpl(String attribute, Class<T> type, Callable<T> fallback) {
        @SuppressWarnings("unchecked")
        Callable<Object> objFallback = (Callable<Object>) fallback;
        Object res = getAttributeImpl(attribute, EmptyMetaField.INSTANCE, objFallback);
        return Json.getMapper().convert(res, type);
    }

    private Object getAttributeImpl(String attribute, MetaField field, Callable<Object> fallback) {

        ParameterizedAttsMixin mixin = mixins.get(attribute);

        if (mixin != null) {
            try {
                return computeAttribute(mixin, attribute, field);
            } catch (Exception e) {
                log.error("mixin error", e);
            }
        }

        ComputedAttribute computedAtt = computedAtts.get(attribute);
        if (computedAtt != null) {
            try {
                return computeAttribute(computedAtt);
            } catch (Exception e) {
                log.error("computed attribute error", e);
            }
        }

        try {
            return fallback.call();
        } catch (Exception e) {
            ExceptionUtils.throwException(e);
        }
        return null;
    }

    private Object computeAttribute(ParameterizedAttsMixin mixin, String attribute, MetaField field) throws Exception {

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
        return mixins.containsKey(name) || computedAtts.containsKey(name) || super.has(name);
    }
}
