package ru.citeck.ecos.records3.record.operation.meta.schema.resolver;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.LibsUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.RecordConstants;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.AttFuncValue;
import ru.citeck.ecos.records3.record.operation.meta.value.HasCollectionView;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValuesConverter;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcService;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcessorDef;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.EmptyValue;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.MetaEdgeValue;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.source.common.AttMixin;
import ru.citeck.ecos.records3.source.common.AttValueCtx;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class AttSchemaResolver {

    public final static String CTX_SOURCE_ID_KEY = "ctx-source-id";

    private final static String PROC_ATT_ALIAS_PREFIX = "__proc_att_";

    private final AttValuesConverter attValuesConverter;
    private final AttProcService attProcService;

    private final AttSchemaReader attSchemaReader;
    private final DtoSchemaReader dtoSchemaReader;

    private final RecordsServiceFactory serviceFactory;

    // todo: move types logic to this resolver
    // private final RecordTypeService recordTypeService;

    public AttSchemaResolver(RecordsServiceFactory factory) {
        serviceFactory = factory;
        attValuesConverter = factory.getAttValuesConverter();
        attProcService = factory.getAttProcService();
        attSchemaReader = factory.getAttSchemaReader();
        dtoSchemaReader = factory.getDtoSchemaReader();
    }

    public DataValue resolve(Object value, SchemaRootAtt attribute) {
        return resolve(value, attribute, false);
    }

    public DataValue resolve(Object value, SchemaRootAtt attribute, List<AttMixin> mixins) {
        return resolve(ResolveArgs.create()
                .setValues(Collections.singletonList(value))
                .setAtt(attribute)
                .setMixins(mixins)
                .build()
        ).stream()
            .map(v -> v.get(attribute.getAttribute().getAliasForValue()))
            .findFirst()
            .orElseThrow(() -> new ResolveException("Resolving failed. Att: " + attribute + " Value: " + value));
    }

    public DataValue resolve(Object value, SchemaRootAtt attribute, boolean rawAtts) {
        ObjectData result = resolve(value, Collections.singletonList(attribute), rawAtts);
        return result.get(attribute.getAttribute().getAliasForValue());
    }

    public ObjectData resolve(Object value, List<SchemaRootAtt> schema, boolean rawAtts) {
        return resolve(b -> {
            b.setValues(Collections.singletonList(value));
            b.setAtts(schema);
            b.setRawAtts(rawAtts);
        }).stream()
            .findFirst()
            .orElseThrow(() -> new ResolveException("Resolving error"));
    }

    public List<ObjectData> resolve(List<Object> values, List<SchemaRootAtt> schema) {
        return resolve(b -> {
            b.setValues(values);
            b.setAtts(schema);
        });
    }

    public List<ObjectData> resolve(Consumer<ResolveArgs.Builder> args) {
        ResolveArgs.Builder builder = ResolveArgs.create();
        args.accept(builder);
        return resolve(builder.build());
    }

    public List<ObjectData> resolve(ResolveArgs args) {
        AttContext context = AttContext.getCurrent();
        if (context == null) {
            return AttContext.doInContext(serviceFactory, ctx -> resolveInAttCtx(args));
        } else {
            return resolveInAttCtx(args);
        }
    }

    private List<ObjectData> resolveInAttCtx(ResolveArgs args) {

        List<Object> values = args.getValues();
        List<SchemaRootAtt> schemaAttsToLoad = args.getAttributes();

        if (!args.isRawAtts()) {

            Set<String> attributesToLoad = new HashSet<>();

            for (SchemaRootAtt rootAtt : schemaAttsToLoad) {
                attributesToLoad.addAll(attProcService.getAttsToLoad(rootAtt.getProcessors()));
            }

            if (!attributesToLoad.isEmpty()) {

                Map<String, String> procAtts = new HashMap<>();

                for (String att : attributesToLoad) {
                    procAtts.put(PROC_ATT_ALIAS_PREFIX + att, att);
                }

                schemaAttsToLoad = new ArrayList<>(schemaAttsToLoad);
                schemaAttsToLoad.addAll(attSchemaReader.readRoot(procAtts));
            }
        }

        List<SchemaRootAtt> schemaAtts = schemaAttsToLoad;
        ResolveContext context = new ResolveContext(attValuesConverter, args.getMixins());

        List<ValueContext> attValues = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            RecordRef ref = args.getValueRefs().isEmpty() ? RecordRef.EMPTY : args.getValueRefs().get(i);
            attValues.add(context.toRootValueContext(values.get(i), ref));
        }

        List<SchemaAtt> attsToResolve = schemaAtts.stream()
            .map(SchemaRootAtt::getAttribute)
            .collect(Collectors.toList());

        List<SchemaAtt> simpleAtts = simplifySchema(attsToResolve);
        List<Map<String, Object>> result = resolveRoot(attValues, simpleAtts, context);
        result = resolveWithAliases(result, attsToResolve);

        return result.stream()
            .map(v -> postProcess(v, schemaAtts, args.isRawAtts()))
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> resolveWithAliases(List<Map<String, Object>> values, List<SchemaAtt> atts) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> value : values) {
            result.add(resolveMapWithAliases(value, atts));
        }
        return result;
    }

    private Map<String, Object> resolveMapWithAliases(Map<String, Object> value, List<SchemaAtt> atts) {

        Object resolved = resolveWithAliases(value, atts, false);
        if (resolved instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) resolved;
            return result;
        }
        throw new IllegalStateException("Expected Map, but found " + resolved + ". Value: " + value + " atts: " + atts);
    }

    private Object resolveWithAliases(Object value, List<SchemaAtt> atts, boolean isMultiple) {

        if (value == null || atts.isEmpty()) {
            return value;
        }
        if (isMultiple) {
            return ((List<?>) value)
                .stream()
                .map(v -> resolveWithAliases(v, atts, false))
                .collect(Collectors.toList());
        } else {
            if (value instanceof List) {
                List<?> valueList = (List<?>) value;
                if (valueList.isEmpty()) {
                    return null;
                }
                value = valueList.get(0);
            }
        }
        if (value instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            for (SchemaAtt att : atts) {
                Object attValue = ((Map<?, ?>) value).get(att.getName());
                result.put(att.getAliasForValue(), resolveWithAliases(attValue, att.getInner(), att.isMultiple()));
            }
            return result;
        } else {
            throw new IllegalStateException("Unknown value: " + value + ". Atts: " + atts);
        }
    }

    private List<SchemaAtt> simplifySchema(List<SchemaAtt> schema) {

        if (schema.isEmpty()) {
            return schema;
        }

        Map<String, SchemaAtt.Builder> result = new HashMap<>();
        for (SchemaAtt att : schema) {
            SchemaAtt.Builder resAtt = result.get(att.getName());
            if (resAtt == null) {
                resAtt = att.modify().setAlias(att.getName());
                result.put(att.getName(), resAtt);
            } else {
                resAtt.setMultiple(resAtt.isMultiple() || att.isMultiple());
                List<SchemaAtt> innerAtts = new ArrayList<>();
                innerAtts.addAll(resAtt.getInner());
                innerAtts.addAll(att.getInner());
                resAtt.setInner(innerAtts);
            }
        }

        List<SchemaAtt> resultAtts = new ArrayList<>();
        for (SchemaAtt.Builder att : result.values()) {
            att.setInner(simplifySchema(att.getInner()));
            resultAtts.add(att.build());
        }

        return resultAtts;
    }

    private ObjectData postProcess(Map<String, Object> data, List<SchemaRootAtt> schemaAtts, boolean isRawAtts) {

        // processors will be ignored if attributes is not flat (raw)
        if (isRawAtts) {
            return ObjectData.create(data);
        }

        ObjectData flatData = ObjectData.create(toFlatMap(data, schemaAtts));
        ObjectData procData = ObjectData.create();

        flatData.forEach((k, v) -> {
            if (k.startsWith(PROC_ATT_ALIAS_PREFIX)) {
                procData.set(k.replaceFirst(PROC_ATT_ALIAS_PREFIX, ""), v);
            }
        });

        for (SchemaRootAtt rootAtt : schemaAtts) {

            SchemaAtt schemaAtt = rootAtt.getAttribute();

            if (schemaAtt.getAlias().startsWith(PROC_ATT_ALIAS_PREFIX)) {
                flatData.remove(schemaAtt.getAlias());
                continue;
            }

            List<AttProcessorDef> processors = rootAtt.getProcessors();
            if (processors.isEmpty()) {
                continue;
            }

            String alias = rootAtt.getAttribute().getAliasForValue();
            DataValue value = flatData.get(alias);

            value = attProcService.process(procData, value, processors);

            flatData.set(alias, value);
        }

        return flatData;
    }

    private Map<String, Object> toFlatMap(Map<String, Object> data, List<SchemaRootAtt> schema) {

        Map<String, Object> flatRes = new LinkedHashMap<>();

        for (SchemaRootAtt rootAtt : schema) {
            SchemaAtt att = rootAtt.getAttribute();
            String alias = att.getAliasForValue();
            flatRes.put(alias, toFlatObj(data.get(alias), att));
        }

        return flatRes;
    }

    private Object toFlatObj(Object data, SchemaAtt schema) {
        return toFlatObj(data, schema, schema.isMultiple());
    }

    private Object toFlatObj(Object data, SchemaAtt schema, boolean multiple) {

        if (multiple && data instanceof Collection) {
            List<Object> res = new ArrayList<>();
            for (Object value : (Collection<?>) data) {
                res.add(toFlatObj(value, schema, false));
            }
            return res;
        }

        List<SchemaAtt> innerAtts = schema.getInner();

        if (!(data instanceof Map)) {
            return data;
        }
        Map<?, ?> dataMap = (Map<?, ?>) data;
        if (innerAtts.size() > 1) {
            Map<String, Object> resMap = new LinkedHashMap<>();
            for (SchemaAtt att : innerAtts) {
                String alias = att.getAliasForValue();
                resMap.put(alias, toFlatObj(dataMap.get(alias), att));
            }
            return resMap;
        } else if (innerAtts.size() == 1) {
            SchemaAtt innerAtt = innerAtts.get(0);
            return toFlatObj(dataMap.get(innerAtt.getAliasForValue()), innerAtt);
        }
        return data;
    }

    private List<Map<String, Object>> resolveRoot(List<ValueContext> values,
                                                  List<SchemaAtt> attributes,
                                                  ResolveContext context) {
        return values.stream()
            .map(v -> resolveRoot(v, attributes, context))
            .collect(Collectors.toList());
    }

    private Map<String, Object> resolveRoot(ValueContext value,
                                            List<SchemaAtt> attributes,
                                            ResolveContext context) {

        ValueContext rootBefore = context.getRootValue();
        context.setRootValue(value);
        try {
            return resolve(value, attributes, context);
        } finally {
            context.setRootValue(rootBefore);
        }
    }

    private List<Map<String, Object>> resolve(List<ValueContext> values,
                                              List<SchemaAtt> attributes,
                                              ResolveContext context) {
        return values.stream()
            .map(v -> resolve(v, attributes, context))
            .collect(Collectors.toList());
    }

    private Map<String, Object> resolve(ValueContext value,
                                        List<SchemaAtt> attributes,
                                        ResolveContext context) {

        Map<String, Object> result = new LinkedHashMap<>();
        AttContext attContext = context.getAttContext();
        String currentValuePath = context.getPath();
        Set<String> disabledMixinPaths = context.getDisabledMixinPaths();

        for (SchemaAtt att : attributes) {

            String attPath;
            if (currentValuePath.isEmpty()) {
                attPath = att.getName();
            } else {
                attPath = currentValuePath + (!att.isScalar() ? "." : "") + att.getName();
            }
            context.setPath(attPath);

            attContext.setSchemaAtt(att);

            AttMixin mixin = null;

            for (AttMixin attMixin : context.getMixins()) {
                if (attMixin.getProvidedAtts().contains(attPath)) {
                    mixin = attMixin;
                    break;
                }
            }

            Object attValue;
            if (mixin != null && !disabledMixinPaths.contains(attPath)) {
                disabledMixinPaths.add(attPath);
                try {
                    attValue = mixin.getAtt(attPath, new AttValueResolveCtx(
                        currentValuePath,
                        context,
                        context.getRootValue()));
                } catch (Exception e) {
                    log.error("Resolving error. Path: " + attPath, e);
                    attValue = null;
                } finally {
                    disabledMixinPaths.remove(attPath);
                }
            } else {
                attValue = value.resolve(attContext);
            }

            List<Object> attValues = toList(attValue);
            String alias = att.getAliasForValue();

            if (att.isMultiple()) {

                if (att.isScalar()) {
                    result.put(alias, attValue);
                } else {
                    List<ValueContext> values = attValues.stream()
                        .map(context::toValueContext)
                        .collect(Collectors.toList());
                    result.put(alias, resolve(values, att.getInner(), context));
                }

            } else {

                if (attValues.isEmpty()) {
                    result.put(alias, null);
                } else {
                    if (att.isScalar()) {
                        result.put(alias, attValues.get(0));
                    } else {
                        ValueContext valueContext = context.toValueContext(attValues.get(0));
                        result.put(alias, resolve(valueContext, att.getInner(), context));
                    }
                }
            }
        }

        context.setPath(currentValuePath);

        return result;
    }

    private List<Object> toList(Object rawValue) {

        List<Object> result;

        if (isNull(rawValue)) {

            result = Collections.emptyList();

        } else if (rawValue instanceof HasCollectionView) {

            result = new ArrayList<>(((HasCollectionView<?>) rawValue).getCollectionView());

        } else if (rawValue instanceof Collection<?>) {

            result = new ArrayList<>((Collection<?>) rawValue);

        } else if (rawValue.getClass().isArray()) {

            if (byte[].class.equals(rawValue.getClass())) {

                result = Collections.singletonList(rawValue);

            } else {

                int length = Array.getLength(rawValue);

                if (length == 0) {

                    result = Collections.emptyList();

                } else {

                    result = new ArrayList<>(length);
                    for (int i = 0; i < length; i++) {
                        result.add(Array.get(rawValue, i));
                    }
                }
            }

        } else {

            result = Collections.singletonList(rawValue);
        }

        return result;
    }

    private boolean isNull(Object rawValue) {

        if (rawValue == null
            || rawValue instanceof RecordRef && RecordRef.isEmpty((RecordRef) rawValue)
            || rawValue instanceof DataValue && ((DataValue) rawValue).isNull()) {

            return true;
        }

        if (rawValue instanceof JsonNode) {
            JsonNode node = (JsonNode) rawValue;
            return node.isNull() || node.isMissingNode();
        }

        if (LibsUtils.isJacksonPresent()) {
            if (rawValue instanceof com.fasterxml.jackson.databind.JsonNode) {
                com.fasterxml.jackson.databind.JsonNode node = (com.fasterxml.jackson.databind.JsonNode) rawValue;
                return node.isNull() || node.isMissingNode();
            }
        }

        return false;
    }

    @RequiredArgsConstructor
    private static class ValueContext {

        public static final ValueContext EMPTY = new ValueContext(EmptyValue.INSTANCE, RecordRef.EMPTY, null);

        private final AttValue value;
        private final RecordRef valueRef;
        private final String ctxSourceId;
        private RecordRef computedRef;

        public Object resolve(AttContext attContext) {

            SchemaAtt schemaAtt = attContext.getSchemaAtt();
            String name = schemaAtt.getName();
            boolean isScalar = schemaAtt.isScalar();

            if (log.isTraceEnabled()) {
            }
            log.info("Resolve " + schemaAtt);

            Object res;
            attContext.setSchemaAttWasRequested(false);
            try {
                res = resolveImpl(name, isScalar);
            } catch (Exception e) {
                log.error("Attribute resolving error. Attribute: " + name + " Value: " + value + " ", e);
                res = null;
            }

            if (log.isTraceEnabled()) {
            }
            log.info("Result: " + res);

            return res;
        }

        private Object resolveImpl(String attribute, boolean isScalar) throws Exception {

            if (RecordConstants.ATT_NULL.equals(attribute)) {
                return null;
            }

            if (isScalar) {
                return getScalar(attribute);
            }

            switch (attribute) {
                case RecordConstants.ATT_TYPE:
                case RecordConstants.ATT_ECOS_TYPE:
                    return value.getTypeRef();
                case RecordConstants.ATT_AS:
                    return new AttFuncValue(value::getAs);
                case RecordConstants.ATT_HAS:
                    return new AttFuncValue(value::has);
                case RecordConstants.ATT_EDGE:
                    return new AttFuncValue(a -> new MetaEdgeValue(value.getEdge(a)));
                default:
                    if (attribute.startsWith("\\_")) {
                        attribute = attribute.substring(1);
                    }
                    return value.getAtt(attribute);
            }
        }

        public String getLocalId() {
            return getValueRef().getId();
        }

        public RecordRef getValueRef() {
            if (RecordRef.isNotEmpty(valueRef)) {
                return valueRef;
            }
            if (computedRef == null) {
                String ctxSourceId = this.ctxSourceId == null ? "" : this.ctxSourceId;
                Object id = value.getId();
                if (id == null || (id instanceof String) && StringUtils.isBlank((String) id)) {
                    computedRef = RecordRef.create(ctxSourceId, UUID.randomUUID().toString());
                } else if (id instanceof RecordRef) {
                    computedRef = (RecordRef) id;
                } else {
                    computedRef = RecordRef.create(ctxSourceId, String.valueOf(id));
                }
                if (!ctxSourceId.isEmpty() && computedRef.getSourceId().isEmpty()) {
                    computedRef = RecordRef.create(ctxSourceId, computedRef.getId());
                }
            }
            return computedRef;
        }

        private Object getScalar(String scalar) throws Exception {

            switch (scalar) {
                case "?str":
                    return value.getString();
                case "?disp":
                    return value.getDisplayName();
                case "?id":
                case "?assoc":
                    return getValueRef();
                case "?localId":
                    return getLocalId();
                case "?type":
                    return value.getTypeRef();
                case "?num":
                    return value.getDouble();
                case "?bool":
                    return value.getBool();
                case "?json":
                    Object json = value.getJson();
                    if (json instanceof String) {
                        json = Json.getMapper().read((String) json);
                    } else {
                        json = Json.getMapper().toJson(json);
                    }
                    return json;
                default:
                    throw new ResolveException("Unknown scalar type: '" + scalar + "'");
            }
        }
    }

    @RequiredArgsConstructor
    private static class ResolveContext {

        private final AttValuesConverter converter;

        @Getter
        @NotNull
        private final AttContext attContext = AttContext.getCurrentNotNull();
        private final RequestContext reqContext = RequestContext.getCurrentNotNull();

        @Getter
        @NotNull
        private final List<AttMixin> mixins;

        @Getter
        private final Set<String> disabledMixinPaths = new HashSet<>();

        @Getter
        @Setter
        private String path = "";

        @Getter
        @Setter
        private ValueContext rootValue;

        @NotNull
        private AttValue convertToMetaValue(@NotNull Object value) {
            if (value instanceof AttValue) {
                return (AttValue) value;
            }
            return converter.toAttValue(value);
        }

        @NotNull
        ValueContext toRootValueContext(@NotNull Object value, RecordRef valueRef) {
            return new ValueContext(convertToMetaValue(value), valueRef, reqContext.getVar(CTX_SOURCE_ID_KEY));
        }

        @NotNull
        ValueContext toValueContext(@Nullable Object value) {
            if (value == null) {
                return ValueContext.EMPTY;
            }
            return new ValueContext(convertToMetaValue(value), RecordRef.EMPTY, null);
        }
    }

    @RequiredArgsConstructor
    public class AttValueResolveCtx implements AttValueCtx {

        private final String basePath;
        private final ResolveContext resolveCtx;
        private final ValueContext valueCtx;

        @NotNull
        @Override
        public RecordRef getRef() throws Exception {
            return valueCtx.getValueRef();
        }

        @NotNull
        @Override
        public String getLocalId() throws Exception {
            return valueCtx.getLocalId();
        }

        @NotNull
        @Override
        public DataValue getAtt(@NotNull String attribute) {
            ObjectData atts = getAtts(Collections.singletonMap("k", attribute));
            return atts.get("k");
        }

        @Override
        public <T> T getAtts(@NotNull Class<T> type) {
            List<SchemaRootAtt> attributes = dtoSchemaReader.read(type);
            return dtoSchemaReader.instantiate(type, getAtts(attributes));
        }

        @NotNull
        @Override
        public ObjectData getAtts(@NotNull Map<String, String> attributes) {
            return getAtts(attSchemaReader.readRoot(attributes));
        }

        @NotNull
        private ObjectData getAtts(@NotNull List<SchemaRootAtt> schemaAtts) {

            String attPathBefore = resolveCtx.getPath();
            resolveCtx.setPath(basePath);

            try {

                List<SchemaAtt> attsToResolve = schemaAtts.stream()
                    .map(SchemaRootAtt::getAttribute)
                    .collect(Collectors.toList());

                List<SchemaAtt> simpleAtts = simplifySchema(attsToResolve);
                Map<String, Object> result = resolve(valueCtx, simpleAtts, resolveCtx);
                result = resolveMapWithAliases(result, attsToResolve);

                return postProcess(result, schemaAtts, false);
            } finally {
                resolveCtx.setPath(attPathBefore);
            }
        }
    }
}
