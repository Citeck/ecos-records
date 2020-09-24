package ru.citeck.ecos.records3.record.operation.meta.schema.resolver;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.LibsUtils;
import ru.citeck.ecos.records3.RecordConstants;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.graphql.meta.value.AttFuncValue;
import ru.citeck.ecos.records3.graphql.meta.value.HasCollectionView;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcService;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcessorDef;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttSchemaReader;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class AttSchemaResolver {

    private static final List<Class<?>> FORCE_CACHE_TYPES = Collections.singletonList(
        String.class
    );

    private final static String PROC_ATT_ALIAS_PREFIX = "__proc_att_";

    private final MetaValuesConverter metaValuesConverter;
    private final AttProcService attProcService;

    private final AttSchemaReader attSchemaReader = new AttSchemaReader();

    // todo: move types logic to this resolver
    // private final RecordTypeService recordTypeService;

    public AttSchemaResolver(RecordsServiceFactory factory) {
        metaValuesConverter = factory.getMetaValuesConverter();
        attProcService = factory.getAttProcService();
    }

    public DataValue resolve(Object value, SchemaRootAtt attribute) {
        return resolve(value, attribute, false);
    }

    public DataValue resolve(Object value, SchemaRootAtt attribute, boolean raw) {
        ObjectData result = resolve(value, Collections.singletonList(attribute), raw);
        return result.get(attribute.getAttribute().getAliasForValue());
    }

    public ObjectData resolve(Object value, List<SchemaRootAtt> schema, boolean raw) {
        return resolve(Collections.singletonList(value), schema, raw)
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResolveException("Resolving error"));
    }

    public List<ObjectData> resolve(List<Object> values, List<SchemaRootAtt> schema) {
        return resolve(values, schema, false);
    }

    public List<ObjectData> resolve(List<Object> values, List<SchemaRootAtt> schema, boolean raw) {

        List<Map<String, Object>> result = AttContext.doInContext(attContext -> {

            ResolveContext context = new ResolveContext(metaValuesConverter, attContext);

            List<SchemaAtt> innerAtts = schema.stream()
                .map(SchemaRootAtt::getAttribute)
                .collect(Collectors.toList());

            attContext.setSchemaAtt(new SchemaAtt("ROOT", "ROOT", false, innerAtts));

            List<ValueContext> metaValues = values.stream()
                .map(context::toValueContext)
                .collect(Collectors.toList());

            return resolveRoot(metaValues, schema, context);
        });

        return result.stream()
            .map(v -> postProcess(v, schema, raw))
            .collect(Collectors.toList());
    }

    private ObjectData postProcess(Map<String, Object> data, List<SchemaRootAtt> schema, boolean raw) {

        if (raw) {
            return ObjectData.create(data);
        }

        ObjectData flatData = ObjectData.create(toFlatMap(data, schema));

        for (SchemaRootAtt rootAtt : schema) {

            List<AttProcessorDef> processors = rootAtt.getProcessors();
            if (processors.isEmpty()) {
                continue;
            }

            String alias = rootAtt.getAttribute().getAliasForValue();

            DataValue value = flatData.get(alias);
            value = attProcService.process(flatData, value, processors, rootAtt.getType());

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

        for (SchemaAtt att : attributes) {

            attContext.setSchemaAtt(att);
            List<Object> attValue = toList(value.resolve(attContext));

            String alias = att.getAliasForValue();
            boolean isScalar = att.getName().charAt(0) == '.';

            if (att.isMultiple()) {

                if (isScalar) {
                    result.put(alias, attValue);
                } else {
                    List<ValueContext> values = attValue.stream()
                        .map(context::toValueContext)
                        .collect(Collectors.toList());
                    result.put(alias, resolve(values, att.getInner(), context));
                }

            } else {

                if (attValue.isEmpty()) {
                    result.put(alias, null);
                } else {
                    if (isScalar) {
                        result.put(alias, attValue.get(0));
                    } else {
                        ValueContext valueContext = context.toValueContext(attValue.get(0));
                        result.put(alias, resolve(valueContext, att.getInner(), context));
                    }
                }
            }
        }

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

    @AllArgsConstructor
    private static class ValueContext {

        private final MetaValue value;
        private final Map<String, Object> attributesCache = new HashMap<>();

        public Object resolve(AttContext attContext) {

            String name = attContext.getSchemaAtt().getName();

            Object res;
            if (attributesCache.containsKey(name)) {
                res = attributesCache.get(name);
            } else {
                attContext.setSchemaAttWasRequested(false);
                try {
                    res = resolveImpl(name);
                } catch (Exception e) {
                    log.error("Attribute resolving error. Attribute: " + name + " Value: " + value + " ");
                    res = null;
                }
                if (!attContext.isSchemaAttWasRequested()) {
                    attributesCache.put(name, res);
                }
            }

            return res;
        }

        private Object resolveImpl(String attribute) throws Exception {

            if (attribute.charAt(0) == '.') {
                return getScalar(attribute.substring(1));
            }

            switch (attribute) {
                case RecordConstants.ATT_TYPE:
                case RecordConstants.ATT_ECOS_TYPE:
                    return value.getRecordType();
                case RecordConstants.ATT_AS:
                    return new AttFuncValue(value::getAs);
                case RecordConstants.ATT_HAS:
                    return new AttFuncValue(value::has);
                default:
                    if (attribute.startsWith("\\_")) {
                        attribute = attribute.substring(1);
                    }
                    return value.getAttribute(attribute);
            }
        }

        private Object getScalar(String scalar) throws Exception {

            switch (scalar) {
                case "str":
                case "assoc":
                    return value.getString();
                case "disp":
                    return value.getDisplayName();
                case "id":
                    return value.getId();
                case "localId":
                    return value.getLocalId();
                case "type":
                    return value.getRecordType();
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

        private final MetaValuesConverter converter;

        @Getter
        private final AttContext attContext;

        @NotNull
        private MetaValue convertToMetaValue(@NotNull Object value) {
            if (value instanceof MetaValue) {
                return (MetaValue) value;
            }
            return converter.toMetaValue(value);
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

            ValueContext result = new ValueContext(convertToMetaValue(value));

            if (!attContext.isSchemaAttWasRequested()) {
                cache.put(value, result);
            }
            return result;
        }
    }
}
