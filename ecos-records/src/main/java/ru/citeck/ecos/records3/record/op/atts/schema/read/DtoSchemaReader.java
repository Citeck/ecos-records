package ru.citeck.ecos.records3.record.op.atts.schema.read;

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
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records3.record.op.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.proc.AttProcessorDef;
import ru.citeck.ecos.records3.record.op.atts.schema.ScalarType;
import ru.citeck.ecos.records3.record.op.atts.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.schema.SchemaRootAtt;

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
            new ScalarField<>(String.class, ScalarType.DISP),
            new ScalarField<>(Boolean.class, ScalarType.BOOL),
            new ScalarField<>(boolean.class, ScalarType.BOOL),
            new ScalarField<>(Double.class, ScalarType.NUM),
            new ScalarField<>(double.class, ScalarType.NUM),
            new ScalarField<>(Float.class, ScalarType.NUM),
            new ScalarField<>(float.class, ScalarType.NUM),
            new ScalarField<>(Integer.class, ScalarType.NUM),
            new ScalarField<>(int.class, ScalarType.NUM),
            new ScalarField<>(Long.class, ScalarType.NUM),
            new ScalarField<>(long.class, ScalarType.NUM),
            new ScalarField<>(Short.class, ScalarType.NUM),
            new ScalarField<>(short.class, ScalarType.NUM),
            new ScalarField<>(Byte.class, ScalarType.NUM),
            new ScalarField<>(byte.class, ScalarType.NUM),
            new ScalarField<>(Date.class, ScalarType.STR),
            new ScalarField<>(Instant.class, ScalarType.STR),
            new ScalarField<>(MLText.class, ScalarType.JSON),
            new ScalarField<>(JsonNode.class, ScalarType.JSON),
            new ScalarField<>(ObjectNode.class, ScalarType.JSON),
            new ScalarField<>(ArrayNode.class, ScalarType.JSON),
            new ScalarField<>(ObjectData.class, ScalarType.JSON),
            new ScalarField<>(DataValue.class, ScalarType.JSON),
            new ScalarField<>(RecordRef.class, ScalarType.ID),
            new ScalarField<>(Map.class, ScalarType.JSON)
        ).forEach(s -> scalars.put(s.getFieldType(), s));

        if (LibsUtils.isJacksonPresent()) {
            Arrays.asList(
                new ScalarField<>(com.fasterxml.jackson.databind.JsonNode.class, ScalarType.JSON),
                new ScalarField<>(com.fasterxml.jackson.databind.node.ObjectNode.class, ScalarType.JSON),
                new ScalarField<>(com.fasterxml.jackson.databind.node.ArrayNode.class, ScalarType.JSON)
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

    private List<SchemaRootAtt> getAttributesImpl(Class<?> attsClass, Set<Class<?>> visited) {

        if (!visited.add(attsClass)) {
            throw new IllegalArgumentException("Recursive meta fields is not supported! "
                + "Class: " + attsClass + " visited: " + visited);
        }

        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(attsClass);
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

            attributes.add(getAttributeSchema(attsClass,
                writeMethod,
                descriptor.getName(),
                isMultiple,
                scalarField,
                propType,
                visited));

        }

        visited.remove(attsClass);

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
                    .setName("?str")
                    .build());
            } else {
                innerAtts = getAttributes(propType, visited).stream()
                    .map(SchemaRootAtt::getAttribute)
                    .collect(Collectors.toList());
            }
        } else {
            innerAtts = Collections.singletonList(SchemaAtt.create()
                .setName(scalarField.getScalarType().getSchema())
                .build());
        }

        String attNameValue = null;
        AttName attName = getAnnotation(writeMethod, scope, fieldName, AttName.class);
        if (attName != null) {
            attNameValue = attName.value();
        } else {
            MetaAtt metaAtt = getAnnotation(writeMethod, scope, fieldName, MetaAtt.class);
            if (metaAtt != null) {
                attNameValue = metaAtt.value();
            }
        }

        SchemaAtt.Builder att;

        List<AttProcessorDef> processors = Collections.emptyList();

        if (StringUtils.isNotBlank(attNameValue)) {

            AttWithProc attWithProc = attSchemaReader.readProcessors(attNameValue);
            processors = attWithProc.getProcessors();
            attNameValue = attWithProc.getAttribute().trim();

            SchemaAtt schemaAtt = attSchemaReader.readInner(fieldName, attNameValue, innerAtts);
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
        @Getter private final ScalarType scalarType;
    }
}
