package ru.citeck.ecos.records3.record.op.atts.service.schema.resolver;

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
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.meta.util.AttStrUtils;
import ru.citeck.ecos.records2.type.ComputedAtt;
import ru.citeck.ecos.records2.type.RecordTypeService;
import ru.citeck.ecos.records3.record.op.atts.service.proc.AttProcDef;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin;
import ru.citeck.ecos.records3.record.op.atts.service.proc.AttProcService;
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.computed.ComputedAttsService;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.AttFuncValue;
import ru.citeck.ecos.records2.graphql.meta.value.HasCollectionView;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValuesConverter;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.AttEdgeValue;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class AttSchemaResolver {

    public final static String CTX_SOURCE_ID_KEY = "ctx-source-id";


    private final AttValuesConverter attValuesConverter;
    private final AttProcService attProcService;

    private final AttSchemaReader attSchemaReader;
    private final DtoSchemaReader dtoSchemaReader;

    private final RecordsServiceFactory serviceFactory;
    private final RecordTypeService recordTypeService;
    private final ComputedAttsService computedAttsService;

    public AttSchemaResolver(RecordsServiceFactory factory) {
        serviceFactory = factory;
        attValuesConverter = factory.getAttValuesConverter();
        attProcService = factory.getAttProcService();
        attSchemaReader = factory.getAttSchemaReader();
        dtoSchemaReader = factory.getDtoSchemaReader();
        recordTypeService = factory.getRecordTypeService();
        computedAttsService = factory.getComputedAttsService();
    }

    public List<Map<String, Object>> resolve(ResolveArgs args) {
        AttContext context = AttContext.getCurrent();
        if (context == null) {
            return AttContext.doInContext(serviceFactory, ctx -> resolveInAttCtx(args));
        } else {
            return resolveInAttCtx(args);
        }
    }

    private List<Map<String, Object>> resolveInAttCtx(ResolveArgs args) {

        List<Object> values = args.getValues();
        List<SchemaAtt> schemaAttsToLoad = args.getAttributes();

        if (!args.isRawAtts()) {
            List<SchemaAtt> processorsAtts = attProcService.getProcessorsAtts(schemaAttsToLoad);
            if (!processorsAtts.isEmpty()) {
                schemaAttsToLoad = new ArrayList<>(schemaAttsToLoad);
                schemaAttsToLoad.addAll(processorsAtts);
            }
        }

        List<SchemaAtt> schemaAtts = schemaAttsToLoad;
        ResolveContext context = new ResolveContext(attValuesConverter, args.getMixins(), recordTypeService);

        List<ValueContext> attValues = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            RecordRef ref = args.getValueRefs().isEmpty() ? RecordRef.EMPTY : args.getValueRefs().get(i);
            attValues.add(context.toRootValueContext(values.get(i), ref));
        }

        List<SchemaAtt> simpleAtts = simplifySchema(schemaAtts);
        List<Map<String, Object>> result = resolveRoot(attValues, simpleAtts, context);

        return resolveResultsWithAliases(result, schemaAtts, args.isRawAtts());
    }

    private List<Map<String, Object>> resolveResultsWithAliases(List<Map<String, Object>> values,
                                                                List<SchemaAtt> atts,
                                                                boolean rawAtts) {

        if (atts.isEmpty()) {
            return values.stream().map(v -> Collections.<String, Object>emptyMap()).collect(Collectors.toList());
        }

        SchemaAtt currentAtt = SchemaAtt.create()
            .withName("root")
            .withInner(atts)
            .build();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> value : values) {
            result.add(resolveResultWithAliases(currentAtt, value, rawAtts));
        }
        return result;
    }

    private Map<String, Object> resolveResultWithAliases(SchemaAtt currentAtt,
                                                         Map<String, Object> value,
                                                         boolean rawAtts) {

        Object resolved = resolveWithAliases(currentAtt, value, false, rawAtts, true);

        if (resolved instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) resolved;
            return result;
        }
        throw new IllegalStateException("Expected Map, but found " + resolved
            + ". Value: " + value + " atts: " + currentAtt);
    }

    private Object resolveWithAliases(SchemaAtt currentAtt,
                                      Object value,
                                      boolean isMultiple,
                                      boolean rawAtts,
                                      boolean root) {

        List<SchemaAtt> atts = currentAtt.getInner();

        if (value == null || atts.isEmpty()) {
            return value;
        }
        if (isMultiple) {
            return ((List<?>) value)
                .stream()
                .map(v -> resolveWithAliases(currentAtt, v, false, rawAtts, root))
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
            if (!root && !rawAtts && atts.size() == 1 && atts.get(0).getProcessors().isEmpty()) {
                SchemaAtt att = atts.get(0);
                Object attValue = ((Map<?, ?>) value).get(att.getName());
                return resolveWithAliases(att, attValue, att.getMultiple(), false, false);
            }
            Map<String, List<AttProcDef>> processors = new HashMap<>();
            Map<String, Object> result = new LinkedHashMap<>();
            for (SchemaAtt att : atts) {
                Object attValue = ((Map<?, ?>) value).get(att.getName());
                result.put(att.getAliasForValue(),
                    resolveWithAliases(
                        att,
                        attValue,
                        att.getMultiple(),
                        rawAtts,
                        false
                    )
                );
                if (!att.getProcessors().isEmpty()) {
                    processors.put(att.getAliasForValue(), att.getProcessors());
                }
            }
            return attProcService.applyProcessors(result, processors);
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
                resAtt = att.copy()
                    .withAlias(att.getName())
                    .withProcessors(Collections.emptyList());
                result.put(att.getName(), resAtt);
            } else {
                resAtt.withMultiple(resAtt.getMultiple() || att.getMultiple());
                List<SchemaAtt> innerAtts = new ArrayList<>();
                innerAtts.addAll(resAtt.getInner());
                innerAtts.addAll(att.getInner());
                resAtt.withInner(innerAtts);
            }
        }

        List<SchemaAtt> resultAtts = new ArrayList<>();
        for (SchemaAtt.Builder att : result.values()) {
            att.withInner(simplifySchema(att.getInner()));
            resultAtts.add(att.build());
        }

        return resultAtts;
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
        Set<String> disabledComputedPaths = context.getDisabledComputedPaths();

        for (SchemaAtt att : attributes) {

            String attPath;
            if (currentValuePath.isEmpty()) {
                attPath = att.getName();
            } else {
                attPath = currentValuePath + (!att.isScalar() ? "." : "") + att.getName();
            }
            context.setPath(attPath);

            attContext.setSchemaAtt(att);

            Map<String, ComputedAtt> computedAtts = value.getComputedAtts();
            ComputedAtt computedAtt = computedAtts.get(att.getName());

            Object attValue;

            if (computedAtt != null && disabledComputedPaths.add(attPath)) {

                try {
                    ValueContext valueCtx = getContextForDynamicAtt(value, computedAtt.getId());
                    attValue = computedAttsService.compute(new AttValueResolveCtx(
                        currentValuePath,
                        context,
                        valueCtx), computedAtt);
                } catch (Exception e) {
                    String msg = "Resolving error. Path: " + attPath + ". Att: " + computedAtt;
                    context.getReqContext().addMsg(MsgLevel.ERROR, () -> msg);
                    log.error(msg, e);
                    attValue = null;
                } finally {
                    disabledComputedPaths.remove(attPath);
                }

            } else {

                AttMixin mixin = null;
                String mixinPath = null;

                mixinLoop:
                for (AttMixin attMixin : context.getMixins()) {
                    for (String mixinAttPath : attMixin.getProvidedAtts()) {
                        if (mixinAttPath.charAt(0) == '^') {
                            if (attPath.equals(mixinAttPath.substring(1))) {
                                mixin = attMixin;
                                mixinPath = mixinAttPath;
                            }
                        } else if (attPath.endsWith(mixinAttPath)) {
                            if (!currentValuePath.endsWith("_edge") || mixinAttPath.contains("_edge.")) {
                                mixin = attMixin;
                                mixinPath = mixinAttPath;
                            }
                        }
                        if (mixin != null) {
                            break mixinLoop;
                        }
                    }
                }

                if (mixin != null && disabledMixinPaths.add(attPath)) {
                    try {
                        ValueContext mixinValueCtx = getContextForDynamicAtt(value, mixinPath);
                        if (mixinValueCtx == null) {
                            attValue = null;
                        } else {
                            attValue = mixin.getAtt(mixinPath, new AttValueResolveCtx(
                                currentValuePath,
                                context,
                                mixinValueCtx));
                        }
                    } catch (Exception e) {
                        String msg = "Resolving error. Path: " + attPath;
                        context.getReqContext().addMsg(MsgLevel.ERROR, () -> msg);
                        log.error(msg, e);
                        attValue = null;
                    } finally {
                        disabledMixinPaths.remove(attPath);
                    }
                } else {
                    attValue = value.resolve(attContext);
                }
            }

            List<Object> attValues = toList(attValue);
            String alias = att.getAliasForValue();

            if (att.getMultiple()) {

                List<ValueContext> values = attValues.stream()
                    .map(v -> context.toValueContext(value, v))
                    .collect(Collectors.toList());
                result.put(alias, resolve(values, att.getInner(), context));

            } else {

                if (attValues.isEmpty()) {
                    result.put(alias, null);
                } else {
                    if (att.isScalar()) {
                        result.put(alias, attValues.get(0));
                    } else {
                        ValueContext valueContext = context.toValueContext(value, attValues.get(0));
                        result.put(alias, resolve(valueContext, att.getInner(), context));
                    }
                }
            }
        }

        context.setPath(currentValuePath);

        return result;
    }

    @Nullable
    private ValueContext getContextForDynamicAtt(ValueContext value, String path) {

        if (AttStrUtils.indexOf(path, ".") == -1) {
            return value;
        }
        ValueContext valueCtx = value;
        List<String> valuePathList = AttStrUtils.split(path, ".");
        for (int i = 1; i < valuePathList.size(); i++) {
            if (valueCtx == null) {
                break;
            }
            valueCtx = valueCtx.getParent();
        }
        return valueCtx != null && path.contains("?") ? valueCtx.getParent() : valueCtx;
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

    private static class ValueContext {

        private static final ValueContext EMPTY = new ValueContext(
            null,
            EmptyAttValue.INSTANCE,
            RecordRef.EMPTY,
            null,
            null,
            Collections.emptyMap());

        private final AttValue value;
        private final RecordRef valueRef;
        private final String ctxSourceId;
        @Nullable
        private final RequestContext context;
        @Getter
        @Nullable
        private final ValueContext parent;
        private RecordRef computedRef;

        private final Map<String, ComputedAtt> computedAtts;

        public ValueContext(@Nullable ValueContext parent,
                            AttValue value,
                            RecordRef valueRef,
                            String ctxSourceId,
                            @Nullable RequestContext context,
                            Map<String, ComputedAtt> computedAtts) {

            this.value = value;
            this.valueRef = valueRef;
            this.ctxSourceId = ctxSourceId;
            this.context = context;
            this.computedAtts = computedAtts;
            this.parent = parent;
        }

        public Map<String, ComputedAtt> getComputedAtts() {
            return computedAtts;
        }

        public Object resolve(AttContext attContext) {

            SchemaAtt schemaAtt = attContext.getSchemaAtt();
            String name = schemaAtt.getName();
            boolean isScalar = schemaAtt.isScalar();

            if (log.isTraceEnabled()) {
                log.trace("Resolve " + schemaAtt);
            }

            Object res;
            attContext.setSchemaAttWasRequested(false);
            try {
                res = resolveImpl(name, isScalar);
            } catch (Throwable e) {
                String msg = "Attribute resolving error. Attribute: " + name + " Value: " + value;
                if (context != null) {
                    context.addMsg(MsgLevel.ERROR, () -> msg);
                }
                log.error(msg, e);
                res = null;
            }

            if (log.isTraceEnabled()) {
                log.trace("Result: " + res);
            }

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
                    return value.getType();
                case RecordConstants.ATT_AS:
                    return new AttFuncValue(value::as);
                case RecordConstants.ATT_HAS:
                    return new AttFuncValue(value::has);
                case RecordConstants.ATT_EDGE:
                    return new AttFuncValue(a -> new AttEdgeValue(value.getEdge(a)));
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
                    return value.asText();
                case "?disp":
                    return value.getDispName();
                case "?id":
                case "?assoc":
                    return getValueRef();
                case "?localId":
                    return getLocalId();
                case "?num":
                    return value.asDouble();
                case "?bool":
                    return value.asBool();
                case "?json":
                    Object json = value.asJson();
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
        @Getter
        private final RequestContext reqContext = RequestContext.getCurrentNotNull();

        @Getter
        @NotNull
        private final List<AttMixin> mixins;

        private final RecordTypeService recordTypeService;

        @Getter
        private final Set<String> disabledMixinPaths = new HashSet<>();
        @Getter
        private final Set<String> disabledComputedPaths = new HashSet<>();

        @Getter
        @Setter
        private String path = "";

        @Getter
        @Setter
        private ValueContext rootValue;

        @NotNull
        private AttValue convertToAttValue(@NotNull Object value) {
            if (value instanceof AttValue) {
                return (AttValue) value;
            }
            return converter.toAttValue(value);
        }

        @NotNull
        ValueContext toRootValueContext(@NotNull Object value, RecordRef valueRef) {
            AttValue attValue = convertToAttValue(value);
            return new ValueContext(
                null,
                attValue,
                valueRef,
                reqContext.getVar(CTX_SOURCE_ID_KEY),
                reqContext,
                getComputedAtts(null, attValue)
            );
        }

        @NotNull
        ValueContext toValueContext(ValueContext parent, @Nullable Object value) {
            if (value == null) {
                return ValueContext.EMPTY;
            }
            AttValue attValue = convertToAttValue(value);
            return new ValueContext(
                parent,
                attValue,
                RecordRef.EMPTY,
                null,
                reqContext,
                getComputedAtts(parent, attValue)
            );
        }

        private Map<String, ComputedAtt> getComputedAtts(@Nullable ValueContext parent, AttValue value) {

            Map<String, ComputedAtt> computedAtts = new HashMap<>();
            if (parent != null) {
                parent.getComputedAtts().forEach((k, v) -> {
                    int dotIdx = AttStrUtils.indexOf(k, ".");
                    if (dotIdx > 0 && dotIdx < k.length() + 1) {
                        computedAtts.put(k.substring(dotIdx + 1), v);
                    }
                });
            }
            try {
                RecordRef typeRef = value.getType();
                if (RecordRef.isNotEmpty(typeRef)) {
                    for (ComputedAtt att : recordTypeService.getComputedAtts(typeRef)) {
                        computedAtts.put(att.getId(), att);
                    }
                }
            } catch (Exception e) {
                log.error("Computed atts resolving error", e);
            }
            return computedAtts;
        }
    }

    @RequiredArgsConstructor
    public class AttValueResolveCtx implements AttValueCtx {

        private final String basePath;
        private final ResolveContext resolveCtx;
        private final ValueContext valueCtx;

        @NotNull
        @Override
        public RecordRef getRef() {
            return valueCtx.getValueRef();
        }

        @NotNull
        @Override
        public String getLocalId() {
            return valueCtx.getLocalId();
        }

        @NotNull
        @Override
        public DataValue getAtt(@NotNull String attribute) {
            ObjectData atts = getAtts(Collections.singletonMap("k", attribute));
            return atts.get("k");
        }

        @NotNull
        @Override
        public <T> T getAtts(@NotNull Class<T> type) {
            List<SchemaAtt> attributes = dtoSchemaReader.read(type);
            return dtoSchemaReader.instantiate(type, getAtts(attributes));
        }

        @NotNull
        @Override
        public ObjectData getAtts(@NotNull Map<String, ?> attributes) {
            return getAtts(attSchemaReader.read(attributes));
        }

        @NotNull
        private ObjectData getAtts(@NotNull List<SchemaAtt> schemaAtts) {

            String attPathBefore = resolveCtx.getPath();
            resolveCtx.setPath(basePath);

            try {

                SchemaAtt currentAtt = SchemaAtt.create()
                    .withName("root")
                    .withInner(schemaAtts)
                    .build();

                List<SchemaAtt> simpleAtts = simplifySchema(schemaAtts);
                Map<String, Object> result = resolve(valueCtx, simpleAtts, resolveCtx);
                return ObjectData.create(resolveResultWithAliases(currentAtt, result, false));

            } finally {
                resolveCtx.setPath(attPathBefore);
            }
        }
    }
}
