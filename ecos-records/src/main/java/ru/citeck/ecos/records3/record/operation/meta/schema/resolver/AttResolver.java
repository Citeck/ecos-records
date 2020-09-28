package ru.citeck.ecos.records3.record.operation.meta.schema.resolver;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.LibsUtils;
import ru.citeck.ecos.records3.RecordConstants;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.schema.AttSchema;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttSchemaUtils;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.DtoSchemaResolver;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.AttFuncValue;
import ru.citeck.ecos.records3.record.operation.meta.value.HasCollectionView;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValuesConverter;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcService;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcessorDef;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.source.common.AttMixin;
import ru.citeck.ecos.records3.source.common.AttValueCtx;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class AttResolver {

    private static final List<Class<?>> FORCE_CACHE_TYPES = Collections.singletonList(
        String.class
    );

    private final static String PROC_ATT_ALIAS_PREFIX = "__proc_att_";

    private final AttValuesConverter attValuesConverter;
    private final AttProcService attProcService;

    private final AttSchemaReader attSchemaReader;
    private final DtoSchemaResolver dtoSchemaResolver;

    // todo: move types logic to this resolver
    // private final RecordTypeService recordTypeService;

    public AttResolver(RecordsServiceFactory factory) {
        attValuesConverter = factory.getAttValuesConverter();
        attProcService = factory.getAttProcService();
        attSchemaReader = factory.getAttSchemaReader();
        dtoSchemaResolver = factory.getDtoMetaResolver();
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

        List<Object> values = args.getValues();
        List<SchemaRootAtt> schemaAtts = args.getSchema().getAttributes();

        List<Map<String, Object>> result = AttContext.doInContext(attContext -> {

            ResolveContext context = new ResolveContext(attValuesConverter, attContext, args.getMixins());

            List<SchemaAtt> innerAtts = schemaAtts.stream()
                .map(SchemaRootAtt::getAttribute)
                .collect(Collectors.toList());

            attContext.setSchemaAtt(SchemaAtt.create()
                .setName("")
                .setInner(innerAtts)
                .build());

            List<ValueContext> attValues;
            if (args.getValueRefs().isEmpty()) {
                attValues = values.stream()
                    .map(context::toValueContext)
                    .collect(Collectors.toList());
            } else {
                attValues = new ArrayList<>();
                for (int i = 0; i < values.size(); i++) {
                    attValues.add(context.toValueContext(values.get(i), args.getValueRefs().get(i)));
                }
            }

            return resolveRoot(attValues, schemaAtts, context);
        });

        return result.stream()
            .map(v -> postProcess(v, args))
            .collect(Collectors.toList());
    }

    private ObjectData postProcess(Map<String, Object> data, ResolveArgs args) {

        // processors will be ignored if attributes is not flat (raw)
        if (args.isRawAtts()) {
            return ObjectData.create(data);
        }

        List<SchemaRootAtt> schemaAtts = args.getSchema().getAttributes();

        ObjectData flatData = ObjectData.create(toFlatMap(data, schemaAtts));

        for (SchemaRootAtt rootAtt : schemaAtts) {

            List<AttProcessorDef> processors = rootAtt.getProcessors();
            if (processors.isEmpty()) {
                continue;
            }

            String alias = rootAtt.getAttribute().getAliasForValue();

            DataValue value = flatData.get(alias);
            value = attProcService.process(flatData, value, processors, AttSchemaUtils.getFlatAttType(rootAtt));

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
                                                  List<SchemaRootAtt> attributes,
                                                  ResolveContext context) {
        return values.stream()
            .map(v -> resolveRoot(v, attributes, context))
            .collect(Collectors.toList());
    }

    private Map<String, Object> resolveRoot(ValueContext value,
                                            List<SchemaRootAtt> rootAttributes,
                                            ResolveContext context) {

        List<SchemaAtt> attributes = new ArrayList<>();
        Set<String> attributesToLoad = new HashSet<>();

        for (SchemaRootAtt rootAtt : rootAttributes) {
            SchemaAtt attribute = rootAtt.getAttribute();
            attributes.add(attribute);
            attributesToLoad.addAll(attProcService.getAttributesToLoad(rootAtt.getProcessors()));
        }

        if (!attributesToLoad.isEmpty()) {
            //todo
            //attSchemaReader.read()
        }

        return resolve(
            value,
            rootAttributes.stream()
                .map(SchemaRootAtt::getAttribute)
                .collect(Collectors.toList()
        ), context);
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
                attPath = (att.isScalar() ? "?" : "") + att.getName();
            } else {
                attPath = currentValuePath + (att.isScalar() ? "?" : ".") + att.getName();
            }
            context.setPath(attPath);

            attContext.setSchemaAtt(att);

            AttMixin mixin = null;

            for (AttMixin attMixin : context.getMixins()) {
                if (attMixin.isProvides(attPath)) {
                    mixin = attMixin;
                    break;
                }
            }

            Object attValue;
            if (mixin != null && !disabledMixinPaths.contains(attPath)) {
                disabledMixinPaths.add(attPath);
                try {
                    attValue = mixin.getAtt(attPath, new AttValueResolveCtx(currentValuePath, context, value));
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

        private final AttValue value;
        private final RecordRef valueRef;
        private final Map<String, Object> attributesCache = new HashMap<>();

        public Object resolve(AttContext attContext) {

            SchemaAtt schemaAtt = attContext.getSchemaAtt();
            String name = schemaAtt.getName();
            boolean isScalar = schemaAtt.isScalar();
            String cacheKey = attContext.getSchemaAtt().isScalar() ? "?" + name : name;

            Object res;
            if (attributesCache.containsKey(cacheKey)) {
                res = attributesCache.get(cacheKey);
            } else {
                attContext.setSchemaAttWasRequested(false);
                try {
                    res = resolveImpl(name, isScalar);
                } catch (Exception e) {
                    log.error("Attribute resolving error. Attribute: " + name + " Value: " + value + " ");
                    res = null;
                }
                if (!attContext.isSchemaAttWasRequested()) {
                    attributesCache.put(cacheKey, res);
                }
            }

            return res;
        }

        private Object resolveImpl(String attribute, boolean isScalar) throws Exception {

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
                default:
                    if (attribute.startsWith("\\_")) {
                        attribute = attribute.substring(1);
                    }
                    return value.getAtt(attribute);
            }
        }

        public String getId() throws Exception {
            return value.getId();
        }

        public RecordRef getValueRef() throws Exception {
            if (RecordRef.isNotEmpty(valueRef)) {
                return valueRef;
            }
            return value.getRef();
        }

        private Object getScalar(String scalar) throws Exception {

            switch (scalar) {
                case "str":
                case "assoc":
                    return value.getString();
                case "disp":
                    return value.getDispName();
                case "id":
                case "localId":
                    return getId();
                case "ref":
                    return getValueRef();
                case "type":
                    return value.getTypeRef();
                case "num":
                    return value.getDouble();
                case "bool":
                    return value.getBool();
                case "json":
                    return value.getJson();
                default:
                    throw new ResolveException("Unknown scalar type: '" + scalar + "'");
            }
        }
    }

    @RequiredArgsConstructor
    private static class ResolveContext {

        private final Map<Object, ValueContext> valueHashCtxCache = new HashMap<>();
        private final Map<Object, ValueContext> valueIdentityCtxCache = new IdentityHashMap<>();

        private final AttValuesConverter converter;

        @Getter
        @NotNull
        private final AttContext attContext;
        @Getter
        @NotNull
        private final List<AttMixin> mixins;

        @Getter
        private final Set<String> disabledMixinPaths = new HashSet<>();

        @Getter
        @Setter
        private String path = "";

        @NotNull
        private AttValue convertToMetaValue(@NotNull Object value) {
            if (value instanceof AttValue) {
                return (AttValue) value;
            }
            return converter.toAttValue(value);
        }

        @NotNull
        ValueContext toValueContext(@NotNull Object value, RecordRef valueRef) {
            return new ValueContext(convertToMetaValue(value), valueRef);
        }

        @NotNull
        ValueContext toValueContext(@NotNull Object value) {

            Map<Object, ValueContext> cache;
            if (FORCE_CACHE_TYPES.contains(value.getClass())) {
                cache = valueHashCtxCache;
            } else {
                cache = valueIdentityCtxCache;
            }

            if (cache.containsKey(value)) {
                return cache.get(value);
            }

            attContext.setSchemaAttWasRequested(false);

            ValueContext result = new ValueContext(convertToMetaValue(value), RecordRef.EMPTY);

            if (!attContext.isSchemaAttWasRequested()) {
                cache.put(value, result);
            }
            return result;
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
        public String getId() throws Exception {
            return valueCtx.getId();
        }

        @NotNull
        @Override
        public DataValue getAtt(@NotNull String attribute) {
            ObjectData atts = getAtts(Collections.singletonMap("k", attribute));
            return atts.get("k");
        }

        @Override
        public <T> T getAtts(@NotNull Class<T> type) {
            Map<String, String> attributes = dtoSchemaResolver.getAttributes(type);
            return dtoSchemaResolver.instantiateMeta(type, getAtts(attributes));
        }

        @NotNull
        @Override
        public ObjectData getAtts(@NotNull Map<String, String> attributes) {

            AttSchema schema = attSchemaReader.read(attributes);

            String attPathBefore = resolveCtx.getPath();
            resolveCtx.setPath(basePath);

            try {
                Map<String, Object> result = resolve(valueCtx, schema.getAttributes()
                    .stream()
                    .map(SchemaRootAtt::getAttribute)
                    .collect(Collectors.toList()), resolveCtx);

                return postProcess(result, ResolveArgs.create().setSchema(schema).build());
            } finally {
                resolveCtx.setPath(attPathBefore);
            }
        }
    }
}
