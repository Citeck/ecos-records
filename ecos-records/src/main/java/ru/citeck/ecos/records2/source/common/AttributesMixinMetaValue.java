package ru.citeck.ecos.records2.source.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.*;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.atts.service.RecordAttsService;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.DtoSchemaReader;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@SuppressFBWarnings({"NP_BOOLEAN_RETURN_NULL"})
public class AttributesMixinMetaValue extends MetaValueDelegate {

    private final RecordAttsService recordAttsService;
    private final DtoSchemaReader dtoSchemaReader;

    private final Map<String, ParameterizedAttsMixin> mixins;
    private final Map<Object, Object> metaCache;

    private final MetaValuesConverter metaValuesConverter;
    private QueryContext context;

    private boolean initialized = false;

    public AttributesMixinMetaValue(MetaValue impl,
                                    DtoSchemaReader dtoSchemaReader,
                                    RecordAttsService recordAttsService,
                                    MetaValuesConverter metaValuesConverter,
                                    Map<String, ParameterizedAttsMixin> mixins,
                                    Map<Object, Object> metaCache) {
        super(impl);
        this.mixins = mixins;
        this.recordAttsService = recordAttsService;
        this.dtoSchemaReader = dtoSchemaReader;
        this.metaValuesConverter = metaValuesConverter;
        this.metaCache = metaCache != null ? metaCache : new ConcurrentHashMap<>();
    }

    @Override
    public <T extends QueryContext> void init(@NotNull T context, @NotNull MetaField field) {
        initImpl(context, field, true);
    }

    private void initImpl(QueryContext context, MetaField field, boolean initSuper) {

        if (initialized) {
            return;
        }

        if (initSuper) {
            super.init(context, field);
        }

        this.context = context;

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
                RecordAtts resMeta = recordAttsService.getAtts(this, attributes);
                if (resMetaType.isAssignableFrom(RecordMeta.class)) {
                    return new RecordMeta(resMeta);
                }
                if (resMetaType.isAssignableFrom(RecordAtts.class)) {
                    return resMeta;
                }
                return dtoSchemaReader.instantiate(resMetaType, resMeta.getAtts());
            } else if (reqMeta instanceof Class) {
                return recordAttsService.getAtts(this, (Class<?>) reqMeta);
            } else {
                return recordAttsService.getAtts(this, reqMeta.getClass());
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
                    dtoSchemaReader,
                    recordAttsService,
                    metaValuesConverter,
                    mixins,
                    null
                );
                att.initImpl(context, field, false);
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
        if (RecordConstants.ATT_DISP.equals(attribute)) {
            return getDisplayName();
        }
        if (RecordConstants.ATT_STR.equals(attribute)) {
            return getString();
        }

        ParameterizedAttsMixin mixin = mixins.get(attribute);

        if (mixin != null) {
            try {
                return computeAttribute(mixin, attribute, field);
            } catch (Exception e) {
                log.error("mixin error", e);
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
