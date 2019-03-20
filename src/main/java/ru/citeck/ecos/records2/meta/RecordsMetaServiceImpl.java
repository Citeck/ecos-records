package ru.citeck.ecos.records2.meta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.utils.ObjectKeyGenerator;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RecordsMetaServiceImpl implements RecordsMetaService {

    private static final Pattern ATT_WITHOUT_SCALAR = Pattern.compile("(.+\\))([}]+)");

    private static final Log logger = LogFactory.getLog(RecordsMetaServiceImpl.class);

    private Map<Class<?>, ScalarField<?>> scalars = new ConcurrentHashMap<>();
    private Map<Class<?>, Map<String, String>> attributesCache = new ConcurrentHashMap<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    private RecordsMetaGql graphQLService;

    public RecordsMetaServiceImpl(RecordsMetaGql graphQLService) {

        this.graphQLService = graphQLService;

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
                new ScalarField<>(Date.class, "str")
        ).forEach(s -> scalars.put(s.getFieldType(), s));
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(List<?> records, String schema) {
        return new RecordsResult<>(graphQLService.getMeta(records, schema));
    }

    @Override
    public List<RecordMeta> convertToFlatMeta(List<RecordMeta> meta, AttributesSchema schema) {
        return meta.stream()
                   .map(m -> convertToFlatMeta(m, schema))
                   .collect(Collectors.toList());
    }

    private RecordMeta convertToFlatMeta(RecordMeta meta, AttributesSchema schema) {

        ObjectNode attributes = meta.getAttributes();
        ObjectNode flatAttributes = JsonNodeFactory.instance.objectNode();
        Map<String, String> keysMapping = schema.getKeysMapping();

        Iterator<String> fields = attributes.fieldNames();

        while (fields.hasNext()) {
            String key = fields.next();
            String resultKey = keysMapping.get(key);
            if (resultKey == null) {
                continue;
            }
            if ("id".equals(resultKey)) {
                flatAttributes.put(resultKey, meta.getId().toString());
            } else {
                flatAttributes.put(resultKey, toFlatNode(attributes.get(key)));
            }
        }

        RecordMeta recordMeta = new RecordMeta(meta.getId());
        recordMeta.setAttributes(flatAttributes);

        return recordMeta;
    }

    private JsonNode toFlatNode(JsonNode input) {

        JsonNode node = input;

        if (node.isObject() && node.size() > 1) {

            ObjectNode objNode = JsonNodeFactory.instance.objectNode();
            final JsonNode finalNode = node;

            node.fieldNames().forEachRemaining(name ->
                    objNode.put(name, toFlatNode(finalNode.get(name)))
            );

            node = objNode;

        } else if (node.isObject() && node.size() == 1) {

            String fieldName = node.fieldNames().next();
            JsonNode value = node.get(fieldName);

            if ("json".equals(fieldName)) {
                node = value;
            } else {
                node = toFlatNode(value);
            }

        } else if (node.isArray()) {

            ArrayNode newArr = JsonNodeFactory.instance.arrayNode();

            for (JsonNode n : node) {
                newArr.add(toFlatNode(n));
            }

            node = newArr;
        }

        return node;
    }

    @Override
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

    @Override
    public <T> T instantiateMeta(Class<T> metaClass, RecordMeta meta) {
        try {
            return objectMapper.treeToValue(meta.getAttributes(), metaClass);
        } catch (JsonProcessingException e) {
            logger.error("Error while meta instantiating", e);
            return null;
        }
    }

    @Override
    public AttributesSchema createSchema(Map<String, String> attributes) {
        return createSchema(attributes, true);
    }

    private AttributesSchema createSchema(Map<String, String> attributes, boolean generateKeys) {

        if (attributes.isEmpty()) {
            return new AttributesSchema("", Collections.emptyMap());
        }

        StringBuilder schema = new StringBuilder();
        ObjectKeyGenerator keys = new ObjectKeyGenerator();

        Map<String, String> keysMapping = new HashMap<>();

        attributes.forEach((name, path) -> {

            String key = generateKeys ? keys.incrementAndGet() : name;

            keysMapping.put(key, name);
            schema.append(key).append(":");

            if (path.charAt(0) != '.') {
                path = convertAttDefinition(path, "disp", false);
            }

            schema.append(path, 1, path.length());
            schema.append(",");
        });
        schema.setLength(schema.length() - 1);

        return new AttributesSchema(schema.toString(), keysMapping);
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

            attributeSchema = attributeSchema.replaceAll("'", "\"");

            schema.setLength(0);
            char lastChar = attributeSchema.charAt(attributeSchema.length() - 1);

            if (lastChar == '}' || !attributeSchema.startsWith(".att")) {
                attributes.put(descriptor.getName(), attributeSchema);
                continue;
            }

            schema.append(attributeSchema).append("{");

            if (scalarField == null) {

                Map<String, String> propSchema = getAttributes(propType, visited);
                schema.append(createSchema(propSchema, false).getSchema());

            } else {

                schema.append(scalarField.getSchema());
            }

            if (schema.charAt(schema.length() - 1) != '{') {

                schema.append("}");
                attributes.put(descriptor.getName(), schema.toString());

            } else {

                logger.error("Class without attributes: " + propType + " property: " + descriptor.getName());
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
                logger.error("Field not found: " + fieldName, e);
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
                schema = convertAttDefinition(att, null, multiple);
            }
        }
        return schema.replaceAll("'", "\"");
    }

    private String convertAttDefinition(String def, String defaultScalar, boolean multiple) {

        String fieldName = def;
        String scalarField = defaultScalar;

        int questionIdx = fieldName.indexOf('?');
        if (questionIdx >= 0) {
            scalarField = fieldName.substring(questionIdx + 1);
            fieldName = fieldName.substring(0, questionIdx);
        }

        if (fieldName.startsWith("#")) {

            if (scalarField == null) {
                throw new IllegalArgumentException("Illegal attribute: '" + def + "'");
            }

            String inner;
            switch (scalarField) {
                case "options":
                case "distinct":
                    inner = "{title:disp,value:str}";
                    break;
                default:
                    inner = "";
            }

            return ".edge(n:\"" + fieldName.substring(1) + "\"){" + scalarField + inner + "}";

        } else {

            String result = (multiple ? ".atts" : ".att") + "(n:\"" + fieldName + "\")";
            if (scalarField != null) {
                return result + "{" + scalarField + "}";
            } else {
                return result;
            }
        }
    }

    public static class ScalarField<FieldTypeT> {

        private String schema;
        private Class<FieldTypeT> fieldClass;

        public ScalarField(Class<FieldTypeT> fieldClass, String schema) {
            this.schema = schema;
            this.fieldClass = fieldClass;
        }

        public String getSchema() {
            return schema;
        }

        public Class<FieldTypeT> getFieldType() {
            return fieldClass;
        }
    }
}
