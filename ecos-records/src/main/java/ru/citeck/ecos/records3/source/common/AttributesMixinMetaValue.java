package ru.citeck.ecos.records3.source.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.ScriptUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier;
import ru.citeck.ecos.records3.record.operation.meta.value.AttEdge;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.MetaValueDelegate;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValuesConverter;
import ru.citeck.ecos.records3.record.operation.query.QueryContext;
import ru.citeck.ecos.records3.RecordConstants;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.meta.RecordAttsService;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.type.ComputedAttribute;
import ru.citeck.ecos.records3.type.RecordTypeService;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@SuppressFBWarnings({"NP_BOOLEAN_RETURN_NULL"})
public class AttributesMixinMetaValue extends MetaValueDelegate {

    private final RecordAttsService recordsMetaService;
    private final RecordTypeService recordTypeService;

    private final Map<String, ParameterizedAttsMixin> mixins;
    private final Map<Object, Object> metaCache;

    private final Map<String, ComputedAttribute> computedAtts = new HashMap<>();

    private final AttValuesConverter attValuesConverter;
    private QueryContext context;

    private boolean initialized = false;

    public AttributesMixinMetaValue(AttValue impl,
                                    RecordAttsService recordsMetaService,
                                    RecordTypeService recordTypeService,
                                    AttValuesConverter attValuesConverter,
                                    Map<String, ParameterizedAttsMixin> mixins,
                                    Map<Object, Object> metaCache) {
        super(impl);
        this.mixins = mixins;
        this.recordTypeService = recordTypeService;
        this.recordsMetaService = recordsMetaService;
        this.attValuesConverter = attValuesConverter;
        this.metaCache = metaCache != null ? metaCache : new ConcurrentHashMap<>();
    }

    private void initImpl(QueryContext context, SchemaAtt field, boolean initSuper) throws Exception {

        if (initialized) {
            return;
        }

        if (initSuper) {
            //super.init(context, field);
        }

        this.context = context;

        RecordRef typeRef = getTypeRef();

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
    public String getDisplayName() throws Exception {
        return getAttributeImpl(".disp", String.class, super::getDisplayName);
    }

    @Override
    public String getString() throws Exception {
        return getAttributeImpl(".str", String.class, super::getString);
    }

    @Override
    public Double getDouble() throws Exception {
        return getAttributeImpl(".num", Double.class, super::getDouble);
    }

    @Override
    public Boolean getBool() throws Exception {
        return getAttributeImpl(".bool", Boolean.class, super::getBool);
    }

    @Override
    public Object getJson() throws Exception {
        return getAttributeImpl(".json", super::getJson);
    }

    private <R, M> R doWithMeta(ParameterizedAttsMixin mixin,
                                UncheckedFunction<Object, R> action) throws Exception {

        return doWithMeta(mixin.getMetaToRequest(), mixin.getResMetaType(), action);
    }

    private <R> R doWithMeta(Object metaToRequest,
                             Class<?> resMetaType,
                             UncheckedFunction<Object, R> action) throws Exception {

        /*if (AttValue.class.equals(resMetaType)) {
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
                RecordAtts resMeta = recordsMetaService.getMeta(this, attributes);
                if (resMetaType.isAssignableFrom(RecordAtts.class)) {
                    return resMeta;
                }
                return recordsMetaService.instantiateMeta(resMetaType, resMeta);
            } else if (reqMeta instanceof Class) {
                return recordsMetaService.getMeta(this, (Class<?>) reqMeta);
            } else {
                return recordsMetaService.getMeta(this, reqMeta.getClass());
            }
        });

        return action.apply(requiredMeta);*/
        return null;
    }

    @Override
    public Object getAttribute(@NotNull String attribute) {
        /*Object result = getAttributeImpl(attribute, () -> super.getAttribute(attribute));
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
                att.initImpl(context, field, false);
                return att;
            }).collect(Collectors.toList());*/

        return null;
    }

    private <T> T getAttributeImpl(String attribute, Class<T> type, Callable<T> fallback) throws Exception {
        @SuppressWarnings("unchecked")
        Callable<Object> objFallback = (Callable<Object>) fallback;
        Object res = getAttributeImpl(attribute, objFallback);
        return Json.getMapper().convert(res, type);
    }

    private Object getAttributeImpl(String attribute, Callable<Object> fallback) throws Exception {

        if (RecordConstants.ATT_ECOS_TYPE.equals(attribute)
            || RecordConstants.ATT_TYPE.equals(attribute)) {

            return getTypeRef();
        }

        ParameterizedAttsMixin mixin = mixins.get(attribute);

        if (mixin != null) {
            try {
                return computeAttribute(mixin, attribute);
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

    private Object computeAttribute(ParameterizedAttsMixin mixin, String attribute) throws Exception {

        return doWithMeta(mixin, meta -> {

            if (meta == this) {

                AttValue implMeta = getImpl();

                return mixin.getAttribute(attribute, new MetaValueDelegate(this) {
                    @Override
                    public Object getAttribute(@NotNull String name) throws Exception {
                        if (attribute.equals(name)) {
                            return implMeta.getAttribute(name);
                        }
                        return super.getAttribute(name);
                    }
                });
            }

            return mixin.getAttribute(attribute, meta);
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
    public AttEdge getEdge(@NotNull String attribute) throws Exception {

        ParameterizedAttsMixin mixin = mixins.get(attribute);

        if (mixin == null) {
            return super.getEdge(attribute);
        }

        try {

            return doWithMeta(mixin, meta -> {

                AttValue implMeta = getImpl();
                UncheckedSupplier<AttEdge> edgeSupplier = () -> implMeta.getEdge(attribute);

                if (meta == this) {

                    return mixin.getEdge(attribute, new MetaValueDelegate(this) {
                        @Override
                        public AttEdge getEdge(@NotNull String name) throws Exception {
                            if (attribute.equals(name)) {
                                return implMeta.getEdge(name);
                            }
                            return super.getEdge(name);
                        }
                    }, edgeSupplier);
                }

                return mixin.getEdge(attribute, meta, edgeSupplier);
            });

        } catch (Exception e) {
            ExceptionUtils.throwException(e);
            return null;
        }
    }

    @Override
    public boolean has(@NotNull String name) throws Exception {
        return mixins.containsKey(name) || computedAtts.containsKey(name) || super.has(name);
    }
}
