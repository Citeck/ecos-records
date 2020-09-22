package ru.citeck.ecos.records2.meta.schema.resolver;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.LibsUtils;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.AttFuncValue;
import ru.citeck.ecos.records2.graphql.meta.value.HasCollectionView;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class AttSchemaResolver {

    private final MetaValuesConverter metaValuesConverter;

    public AttSchemaResolver(RecordsServiceFactory factory) {
        metaValuesConverter = factory.getMetaValuesConverter();
    }

    public DataValue resolve(Object value, SchemaAtt attribute) {
        return resolve(value, attribute, true);
    }

    public DataValue resolve(Object value, SchemaAtt attribute, boolean flat) {
        ObjectData result = resolve(value, Collections.singletonList(attribute), flat);
        return result.get(attribute.getAliasForValue());
    }

    public ObjectData resolve(Object value, List<SchemaAtt> schema, boolean flat) {
        return resolve(Collections.singletonList(value), schema, flat)
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResolveException("Resolving error"));
    }

    public List<ObjectData> resolve(List<Object> values, List<SchemaAtt> schema) {
        return resolve(values, schema, true);
    }

    public List<ObjectData> resolve(List<Object> values, List<SchemaAtt> schema, boolean flat) {

        ResolveContext context = new ResolveContext(metaValuesConverter);

        List<ValueContext> metaValues = values.stream()
            .map(context::toValueContext)
            .collect(Collectors.toList());

        List<Map<String, Object>> result = resolve(metaValues, schema, context);

        return result.stream()
            .map(v -> ObjectData.create(flat ? toFlatMap(v, schema) : v))
            .collect(Collectors.toList());
    }

    private Map<String, Object> toFlatMap(Map<String, Object> data, List<SchemaAtt> schema) {

        Map<String, Object> flatRes = new LinkedHashMap<>();

        for (SchemaAtt att : schema) {
            String alias = att.getAliasForValue();
            flatRes.put(alias, toFlatObj(data.get(alias), att));
        }

        return flatRes;
    }

    private Object toFlatObj(Object data, SchemaAtt schema) {

        List<SchemaAtt> innerAtts = schema.getInner();
        if (innerAtts.size() != 1 || !(data instanceof Map)) {
            return data;
        }

        SchemaAtt innerAtt = innerAtts.get(0);
        Map<?, ?> dataMap = (Map<?, ?>) data;
        return toFlatObj(dataMap.get(innerAtt.getAliasForValue()), innerAtt);
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

        for (SchemaAtt att : attributes) {

            List<Object> attValue = toList(value.resolve(att.getName()));
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
        private final Map<String, Object> attributes = new HashMap<>();

        public Object resolve(String attribute) {
            return attributes.computeIfAbsent(attribute, this::resolveImpl);
        }

        private Object resolveImpl(String attribute) {

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
                    return new AttFuncValue((attName, field) -> {
                        try {
                            return value.has(attName);
                        } catch (Exception e) {
                            ExceptionUtils.throwException(e);
                            return false;
                        }
                    });
                default:
                    if (attribute.startsWith("\\_")) {
                        attribute = attribute.substring(1);
                    }
                    try {
                        //todo: meta field
                        return value.getAttribute(attribute, EmptyMetaField.INSTANCE);
                    } catch (Exception e) {
                        ExceptionUtils.throwException(e);
                    }
            }

            return null;
        }

        private Object getScalar(String scalar) {

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

        private final Map<Object, MetaValue> metaValueCache = new IdentityHashMap<>();
        private final Map<MetaValue, ValueContext> valueContextCache = new IdentityHashMap<>();

        private final MetaValuesConverter converter;

        @NotNull
        MetaValue toMetaValue(@NotNull Object value) {
            if (value instanceof MetaValue) {
                return (MetaValue) value;
            }
            return metaValueCache.computeIfAbsent(value, converter::toMetaValue);
        }

        @NotNull
        ValueContext toValueContext(@NotNull Object value) {
            return valueContextCache.computeIfAbsent(toMetaValue(value), ValueContext::new);
        }
    }
}
