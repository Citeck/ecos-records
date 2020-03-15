package ru.citeck.ecos.records2.meta;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.LibsUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DtoMetaResolver {

    private static final Pattern ATT_WITHOUT_SCALAR = Pattern.compile("(.+\\))([}]+)");

    private Map<Class<?>, ScalarField<?>> scalars = new ConcurrentHashMap<>();
    private Map<Class<?>, Map<String, String>> attributesCache = new ConcurrentHashMap<>();

    private AttributesMetaResolver attributesMeta;

    public DtoMetaResolver(RecordsServiceFactory factory) {

        attributesMeta = factory.getAttributesMetaResolver();

        Arrays.asList(
            new ScalarField<>(String.class, "disp"),
            new ScalarField<>(Boolean.class, "bool"),
            new ScalarField<>(boolean.class, "bool"),
            new ScalarField<>(Double.class, "num"),
            new ScalarField<>(double.class, "num"),
            new ScalarField<>(Float.class, "num"),
            new ScalarField<>(float.class, "num"),
            new ScalarField<>(Integer.class, "num"),
            new ScalarField<>(int.class, "num"),
            new ScalarField<>(Long.class, "num"),
            new ScalarField<>(long.class, "num"),
            new ScalarField<>(Short.class, "num"),
            new ScalarField<>(short.class, "num"),
            new ScalarField<>(Byte.class, "num"),
            new ScalarField<>(byte.class, "num"),
            new ScalarField<>(Date.class, "str"),
            new ScalarField<>(JsonNode.class, "json"),
            new ScalarField<>(ObjectNode.class, "json"),
            new ScalarField<>(ArrayNode.class, "json"),
            new ScalarField<>(ObjectData.class, "json"),
            new ScalarField<>(DataValue.class, "json"),
            new ScalarField<>(RecordRef.class, "id")
        ).forEach(s -> scalars.put(s.getFieldType(), s));

        if (LibsUtils.isJacksonPresent()) {
            Arrays.asList(
                new ScalarField<>(com.fasterxml.jackson.databind.JsonNode.class, "json"),
                new ScalarField<>(com.fasterxml.jackson.databind.node.ObjectNode.class, "json"),
                new ScalarField<>(com.fasterxml.jackson.databind.node.ArrayNode.class, "json")
            ).forEach(s -> scalars.put(s.getFieldType(), s));
        }
    }

    public Map<String, String> getAttributes(Class<?> metaClass) {
        return getAttributes(metaClass, null);
    }

    private Map<String, String> getAttributes(Class<?> metaClass, Set<Class<?>> visited) {
        Map<String, String> attributes = attributesCache.get(metaClass);
        if (attributes == null) {
            attributes = getAttributesImpl(metaClass, visited != null ? visited : new HashSet<>());
            attributesCache.putIfAbsent(metaClass, attributes);
        }
        return attributes;
    }

    public <T> T instantiateMeta(Class<T> metaClass, ObjectData attributes) {
        return Json.getMapper().convert(attributes, metaClass);
    }

    private Map<String, String> getAttributesImpl(Class<?> metaClass, Set<Class<?>> visited) {

        if (!visited.add(metaClass)) {
            throw new IllegalArgumentException("Recursive meta fields is not supported! "
                + "Class: " + metaClass + " visited: " + visited);
        }

        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(metaClass);
        Map<String, String> attributes = new HashMap<>();

        StringBuilder schema = new StringBuilder();

        for (PropertyDescriptor descriptor : descriptors) {

            Method writeMethod = descriptor.getWriteMethod();

            if (writeMethod == null) {
                continue;
            }

            Class<?> propType = descriptor.getPropertyType();
            boolean isMultiple = false;

            if (List.class.isAssignableFrom(propType) || Set.class.isAssignableFrom(propType)) {
                ParameterizedType parameterType = (ParameterizedType) writeMethod.getGenericParameterTypes()[0];

                Type type = parameterType.getActualTypeArguments()[0];

                if (type instanceof Class) {
                    propType = (Class<?>) parameterType.getActualTypeArguments()[0];
                } else if (type instanceof ParameterizedType) {
                    propType = (Class<?>) ((ParameterizedType) type).getRawType();
                }

                isMultiple = true;
            }

            ScalarField<?> scalarField = scalars.get(propType);

            String attributeSchema = getAttributeSchema(metaClass,
                writeMethod,
                descriptor.getName(),
                isMultiple,
                scalarField);

            schema.setLength(0);
            char lastChar = attributeSchema.charAt(attributeSchema.length() - 1);

            if (lastChar == '}' || !attributeSchema.startsWith(".att")) {
                attributes.put(descriptor.getName(), attributeSchema);
                continue;
            }

            schema.append(attributeSchema).append("{");

            if (scalarField == null) {

                Map<String, String> propSchema = getAttributes(propType, visited);
                schema.append(attributesMeta.createSchema(propSchema, false).getSchema());

            } else {

                schema.append(scalarField.getSchema());
            }

            if (schema.charAt(schema.length() - 1) != '{') {

                int openBraces = StringUtils.countMatches(schema, "{");
                int closeBraces = StringUtils.countMatches(schema, "}");

                while (openBraces > closeBraces++) {
                    schema.append("}");
                }
                attributes.put(descriptor.getName(), schema.toString());

            } else {

                log.error("Class without attributes: " + propType + " property: " + descriptor.getName());
            }
        }

        visited.remove(metaClass);

        return attributes;
    }

    private String getAttributeSchema(Class<?> scope,
                                      Method writeMethod,
                                      String fieldName,
                                      boolean multiple,
                                      ScalarField<?> scalarField) {

        MetaAtt attInfo = writeMethod.getAnnotation(MetaAtt.class);

        if (attInfo == null) {
            Field field;
            try {
                field = scope.getDeclaredField(fieldName);
                if (field != null) {
                    attInfo = field.getAnnotation(MetaAtt.class);
                }
            } catch (NoSuchFieldException e) {
                log.error("Field not found: " + fieldName, e);
            }
        }

        String schema;
        if (attInfo == null || attInfo.value().isEmpty()) {
            if ("id".equals(fieldName)) {
                schema = ".id";
            } else {
                schema = ".att" + (multiple ? "s" : "") + "(n:'" + fieldName + "')";
            }
        } else {
            String att = attInfo.value();
            if (att.startsWith(".")) {

                Matcher matcher = ATT_WITHOUT_SCALAR.matcher(att);
                if (matcher.matches()) {
                    schema = matcher.group(1) + '{' + scalarField.getSchema() + '}' + matcher.group(2);
                } else {
                    schema = att;
                }
            } else {
                schema = attributesMeta.convertAttToGqlFormat(att, null, multiple);
            }
        }
        return schema.replaceAll("'", "\"");
    }

    @AllArgsConstructor
    private static class ScalarField<FieldTypeT> {
        @Getter private Class<FieldTypeT> fieldType;
        @Getter private String schema;
    }
}
