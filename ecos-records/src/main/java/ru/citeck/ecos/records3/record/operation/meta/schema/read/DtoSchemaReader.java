package ru.citeck.ecos.records3.record.operation.meta.schema.read;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.LibsUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.graphql.meta.annotation.AttName;
import ru.citeck.ecos.records3.record.operation.meta.attproc.AttProcessorDef;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class DtoSchemaReader {

    private final Map<Class<?>, ScalarField<?>> scalars = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<SchemaRootAtt>> attributesCache = new ConcurrentHashMap<>();

    private final AttSchemaReader attSchemaReader;

    public DtoSchemaReader(RecordsServiceFactory factory) {

        attSchemaReader = factory.getAttSchemaReader();

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
            new ScalarField<>(Instant.class, "str"),
            new ScalarField<>(MLText.class, "json"),
            new ScalarField<>(JsonNode.class, "json"),
            new ScalarField<>(ObjectNode.class, "json"),
            new ScalarField<>(ArrayNode.class, "json"),
            new ScalarField<>(ObjectData.class, "json"),
            new ScalarField<>(DataValue.class, "json"),
            new ScalarField<>(RecordRef.class, "ref"),
            new ScalarField<>(Map.class, "json")
        ).forEach(s -> scalars.put(s.getFieldType(), s));

        if (LibsUtils.isJacksonPresent()) {
            Arrays.asList(
                new ScalarField<>(com.fasterxml.jackson.databind.JsonNode.class, "json"),
                new ScalarField<>(com.fasterxml.jackson.databind.node.ObjectNode.class, "json"),
                new ScalarField<>(com.fasterxml.jackson.databind.node.ArrayNode.class, "json")
            ).forEach(s -> scalars.put(s.getFieldType(), s));
        }
    }

    @NotNull
    public List<SchemaRootAtt> read(Class<?> attsClass) {
        return getAttributes(attsClass, null);
    }

    public <T> T instantiate(Class<T> metaClass, ObjectData attributes) {
        return Json.getMapper().convert(attributes, metaClass);
    }

    private List<SchemaRootAtt> getAttributes(Class<?> attsClass, Set<Class<?>> visited) {

        List<SchemaRootAtt> attributes = attributesCache.get(attsClass);
        if (attributes == null) {
            attributes = getAttributesImpl(attsClass, visited != null ? visited : new HashSet<>());
            attributesCache.putIfAbsent(attsClass, attributes);
        }
        return attributes;
    }

    private List<SchemaRootAtt> getAttributesImpl(Class<?> metaClass, Set<Class<?>> visited) {

        if (!visited.add(metaClass)) {
            throw new IllegalArgumentException("Recursive meta fields is not supported! "
                + "Class: " + metaClass + " visited: " + visited);
        }

        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(metaClass);
        List<SchemaRootAtt> attributes = new ArrayList<>();

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

            attributes.add(getAttributeSchema(metaClass,
                writeMethod,
                descriptor.getName(),
                isMultiple,
                scalarField,
                propType,
                visited));

        }

        visited.remove(metaClass);

        return attributes;
    }

    private SchemaRootAtt getAttributeSchema(Class<?> scope,
                                             Method writeMethod,
                                             String fieldName,
                                             boolean multiple,
                                             ScalarField<?> scalarField,
                                             Class<?> propType,
                                             Set<Class<?>> visited) {

        List<SchemaAtt> innerAtts;

        if (scalarField == null) {
            if (propType.isEnum()) {
                innerAtts = Collections.singletonList(SchemaAtt.create()
                    .setName("str")
                    .setScalar(true)
                    .build());
            } else {
                innerAtts = getAttributes(propType, visited).stream()
                    .map(SchemaRootAtt::getAttribute)
                    .collect(Collectors.toList());
            }
        } else {
            innerAtts = Collections.singletonList(SchemaAtt.create()
                .setName(scalarField.getSchema())
                .setScalar(true)
                .build());
        }

        AttName attName = getAnnotation(writeMethod, scope, fieldName, AttName.class);

        SchemaAtt.Builder att;

        List<AttProcessorDef> processors = Collections.emptyList();

        String attNameAnn = attName != null ? attName.value() : "";
        if (StringUtils.isNotBlank(attNameAnn)) {

            AttWithProc attWithProc = attSchemaReader.readProcessors(attNameAnn);
            processors = attWithProc.getProcessors();
            attNameAnn = attWithProc.getAttribute().trim();

            SchemaAtt schemaAtt = attSchemaReader.readInner(fieldName, attNameAnn, innerAtts);
            att = schemaAtt.modify();

            if (multiple && !schemaAtt.isMultiple()) {
                SchemaAtt innerAtt = schemaAtt;
                while (!innerAtt.isMultiple() && innerAtt.getInner().size() == 1) {
                    innerAtt = innerAtt.getInner().get(0);
                }
                if (!innerAtt.isMultiple()) {
                    att.setMultiple(true);
                }
            }

        } else {

            att = SchemaAtt.create();
            att.setMultiple(multiple);
            att.setAlias(fieldName);
            att.setName(fieldName);
            att.setInner(innerAtts);
        }

        return new SchemaRootAtt(att.build(), processors);
    }

    private <T extends Annotation> T getAnnotation(Method writeMethod,
                                                   Class<?> scope,
                                                   String fieldName,
                                                   Class<T> type) {

        T annotation = writeMethod.getAnnotation(type);

        if (annotation == null) {
            Field field;
            try {
                field = scope.getDeclaredField(fieldName);
                annotation = field.getAnnotation(type);
            } catch (NoSuchFieldException e) {
                log.error("Field not found: " + fieldName, e);
            }
        }
        return annotation;
    }

    @AllArgsConstructor
    private static class ScalarField<FieldTypeT> {
        @Getter private final Class<FieldTypeT> fieldType;
        @Getter private final String schema;
    }
}
