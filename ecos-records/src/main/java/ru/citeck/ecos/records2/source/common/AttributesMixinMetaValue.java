package ru.citeck.ecos.records2.source.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.ScriptUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.*;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.type.ComputedAttribute;
import ru.citeck.ecos.records2.type.RecordTypeService;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@SuppressFBWarnings({"NP_BOOLEAN_RETURN_NULL"})
public class AttributesMixinMetaValue extends MetaValueDelegate {

    private final RecordsMetaService recordsMetaService;
    private final RecordTypeService recordTypeService;

    private final Map<String, ParameterizedAttsMixin> mixins;
    private final Map<Object, Object> metaCache;

    private final Map<String, ComputedAttribute> computedAtts = new HashMap<>();

    private final MetaValuesConverter metaValuesConverter;
    private QueryContext context;

    private boolean initialized = false;

    public AttributesMixinMetaValue(MetaValue impl,
                                    RecordsMetaService recordsMetaService,
                                    RecordTypeService recordTypeService,
                                    MetaValuesConverter metaValuesConverter,
                                    Map<String, ParameterizedAttsMixin> mixins,
                                    Map<Object, Object> metaCache) {
        super(impl);
        this.mixins = mixins;
        this.recordTypeService = recordTypeService;
        this.recordsMetaService = recordsMetaService;
        this.metaValuesConverter = metaValuesConverter;
        this.metaCache = metaCache != null ? metaCache : new ConcurrentHashMap<>();
    }

    @Override
    public <T extends QueryContext> void init(T context, MetaField field) {

        if (initialized) {
            return;
        }
        super.init(context, field);

        this.context = context;

        RecordRef typeRef = getRecordType();

        if (!RecordRef.isEmpty(typeRef) && !QueryContext.getCurrent().isComputedAttsDisabled()) {

            List<ComputedAttribute> atts = QueryContext.withoutComputedAtts(() ->
                recordTypeService.getComputedAttributes(typeRef));

            if (atts != null) {
                for (ComputedAttribute att : atts) {
                    this.computedAtts.put(att.getId(), att);
                }
            }
        }

        initialized = true;
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
        Object result = getAttributeImpl(attribute, field, () -> super.getAttribute(attribute, field));
        if (result instanceof RecordRef) {
            return result;
        }
        List<MetaValue> metaValues = metaValuesConverter.getAsMetaValues(result, context, field, false);
        return metaValues.stream()
            .map(value -> {
                AttributesMixinMetaValue att = new AttributesMixinMetaValue(value,
                    recordsMetaService,
                    recordTypeService,
                    metaValuesConverter,
                    mixins,
                    null
                );
                att.init(context, field);
                return att;
            }).collect(Collectors.toList());
    }

    private <T> T getAttributeImpl(String attribute, Class<T> type, Callable<T> fallback) {
        @SuppressWarnings("unchecked")
        Callable<Object> objFallback = (Callable<Object>) fallback;
        Object res = getAttributeImpl(attribute, EmptyMetaField.INSTANCE, objFallback);
        return Json.getMapper().convert(res, type);
    }

    private Object getAttributeImpl(String attribute, MetaField field, Callable<Object> fallback) {

        if (RecordConstants.ATT_ECOS_TYPE.equals(attribute)
            || RecordConstants.ATT_TYPE.equals(attribute)) {

            return getRecordType();
        }

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
