package ru.citeck.ecos.records2.meta;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;
import ru.citeck.ecos.records2.meta.attproc.AttProcessor;
import ru.citeck.ecos.records2.meta.attproc.AttProcessorDef;
import ru.citeck.ecos.records2.meta.attproc.FormatAttProcessor;
import ru.citeck.ecos.records2.meta.attproc.PrefixSuffixAttProcessor;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class RecordsMetaServiceImpl implements RecordsMetaService {

    private final RecordsMetaGql graphQLService;
    private final AttributesMetaResolver attributesMetaResolver;
    private final DtoMetaResolver dtoMetaResolver;

    private final Map<String, AttProcessor> attProcessors = new ConcurrentHashMap<>();

    public RecordsMetaServiceImpl(RecordsServiceFactory serviceFactory) {
        graphQLService = serviceFactory.getRecordsMetaGql();
        dtoMetaResolver = serviceFactory.getDtoMetaResolver();
        attributesMetaResolver = serviceFactory.getAttributesMetaResolver();

        register(new FormatAttProcessor());
        register(new PrefixSuffixAttProcessor());
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(List<?> records, String schema) {
        if (StringUtils.isBlank(schema)) {
            schema = "id";
        }
        return new RecordsResult<>(graphQLService.getMeta(records, schema));
    }

    @Override
    public <T> T getMeta(Object record, Class<T> metaClass) {

        RecordsResult<T> meta = getMeta(Collections.singletonList(record), metaClass);
        ErrorUtils.logErrors(meta);

        if (meta.getRecords().isEmpty()) {
            throw new IllegalStateException("Meta can't be received for record "
                                            + record + " and metaClass: " + metaClass);
        }

        return meta.getRecords().get(0);
    }

    @Override
    public <T> RecordsResult<T> getMeta(List<?> records, Class<T> metaClass) {

        Map<String, String> attributes = getAttributes(metaClass);
        if (attributes.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }
        RecordsResult<RecordMeta> meta = getMeta(records, attributes);
        return new RecordsResult<>(meta, m -> instantiateMeta(metaClass, m));
    }

    @Override
    public RecordMeta getMeta(Object record, Collection<String> attributes) {
        Map<String, String> attsMap = new HashMap<>();
        attributes.forEach(att -> attsMap.put(att, att));
        return getMeta(record, attsMap);
    }

    @Override
    public RecordMeta getMeta(Object record, Map<String, String> attributes) {

        RecordsResult<RecordMeta> meta = getMeta(Collections.singletonList(record), attributes);
        ErrorUtils.logErrors(meta);

        if (meta.getRecords().isEmpty()) {
            throw new IllegalStateException("Meta can't be received for record "
                + record + " and attributes: " + attributes);
        }

        return meta.getRecords().get(0);
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(List<?> records, Map<String, String> attributes) {

        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Attributes is empty. Records: " + records);
        }

        AttributesSchema schema = createSchema(attributes);
        RecordsResult<RecordMeta> meta = getMeta(records, schema.getGqlSchema());
        meta.setRecords(convertMetaResult(meta.getRecords(), schema, true));

        return meta;
    }

    @Override
    public List<RecordMeta> convertMetaResult(List<RecordMeta> meta, AttributesSchema schema, boolean flat) {
        return meta.stream()
                   .map(m -> convertMetaResult(m, schema, flat))
                   .collect(Collectors.toList());
    }

    @Override
    public RecordMeta convertMetaResult(RecordMeta meta, AttributesSchema schema, boolean flat) {

        ObjectData attributes = meta.getAttributes();
        ObjectData resultAttributes = ObjectData.create();
        Map<String, AttSchemaInfo> attsInfo = schema.getAttsInfo();

        Map<String, MetaField> subFields = schema.getMetaField().getSubFields();

        Iterator<String> fieldsIt = attributes.fieldNames();
        while (fieldsIt.hasNext()) {
            String nextFieldName = fieldsIt.next();
            processAttribute(
                nextFieldName,
                attsInfo,
                attributes,
                resultAttributes,
                flat,
                subFields.getOrDefault(nextFieldName, EmptyMetaField.INSTANCE)
            );
        }

        RecordMeta recordMeta = new RecordMeta(meta.getId());
        recordMeta.setAttributes(resultAttributes);

        return recordMeta;
    }

    private void processAttribute(String key,
                                  Map<String, AttSchemaInfo> attsInfo,
                                  ObjectData attributes,
                                  ObjectData resultAttributes,
                                  boolean flat,
                                  MetaField metaField) {

        AttSchemaInfo attInfo = attsInfo.get(key);
        if (attInfo == null) {
            return;
        }
        String resultKey = attInfo.getOriginalKey();
        if (StringUtils.isBlank(attInfo.getOriginalKey())) {
            return;
        }

        DataValue value = attributes.get(key);

        if (metaField.getName().equals("json") || !flat) {

            resultAttributes.set(resultKey, value);

        } else {

            JsonNode flatValue = toFlatNode(value.getValue(), metaField);

            if (flatValue.isNull()) {
                List<String> orElseAtts = attInfo.getOrElseAtts();
                for (String orElseAtt : orElseAtts) {
                    char firstChar = orElseAtt.charAt(0);
                    if (firstChar == '"' || firstChar == '\'') {
                        orElseAtt = orElseAtt.substring(1, orElseAtt.length() - 1);
                        if (attInfo.getType() != null && !String.class.equals(attInfo.getType())) {
                            flatValue = Json.getMapper().toJson(
                                DataValue.createStr(orElseAtt).getAs(attInfo.getType())
                            );
                        } else {
                            flatValue = TextNode.valueOf(orElseAtt);
                        }
                        break;
                    } else {
                        MetaField orElseAttField = metaField.getParent().getSubFields().get(orElseAtt);
                        JsonNode orElseValue = toFlatNode(attributes.get(orElseAtt).getValue(), orElseAttField);
                        if (!orElseValue.isNull()) {
                            flatValue = orElseValue;
                            break;
                        }
                    }
                }
            }

            if (!flatValue.isNull() && !attInfo.getProcessors().isEmpty()) {

                DataValue processedValue = DataValue.create(flatValue);
                for (AttProcessorDef procDef : attInfo.getProcessors()) {
                    AttProcessor processor = attProcessors.get(procDef.getType());
                    if (processor == null) {
                        log.error("Attribute processor '"
                            + procDef.getType()
                            + "' is not found! Result will be null");
                        processedValue = DataValue.NULL;
                    } else {
                        Object procResult = processor.process(processedValue, procDef.getArguments());
                        processedValue = DataValue.create(procResult);
                    }
                }
                flatValue = processedValue.getValue();
            }
            resultAttributes.set(resultKey, flatValue);
        }
    }

    @Override
    public Map<String, String> getAttributes(Class<?> metaClass) {
        return dtoMetaResolver.getAttributes(metaClass);
    }

    @Override
    public <T> T instantiateMeta(Class<T> metaClass, RecordMeta flatMeta) {
        return dtoMetaResolver.instantiateMeta(metaClass, flatMeta.getAttributes());
    }

    @Override
    public AttributesSchema createSchema(Map<String, String> attributes) {
        return attributesMetaResolver.createSchema(attributes);
    }

    private JsonNode toFlatNode(JsonNode input, @Nullable MetaField metaField) {

        if (input == null || input.isMissingNode() || input.isNull()) {
            return NullNode.getInstance();
        }

        if (metaField == null || metaField == EmptyMetaField.INSTANCE) {
            return input;
        }

        JsonNode node = input;

        if (node.isObject()) {

            Map<String, MetaField> subFields = metaField.getSubFields();

            if (node.size() > 1) {

                ObjectNode objNode = JsonNodeFactory.instance.objectNode();
                final JsonNode finalNode = node;

                node.fieldNames().forEachRemaining(name ->
                    objNode.set(name, toFlatNode(finalNode.get(name), subFields.get(name)))
                );

                node = objNode;

            } else if (node.size() == 1) {

                String fieldName = node.fieldNames().next();
                JsonNode value = node.get(fieldName);
                MetaField subField = subFields.get(fieldName);

                if (subField == null || "json".equals(subField.getName())) {
                    node = value;
                } else {
                    node = toFlatNode(value, subField);
                }
            }

        } else if (node.isArray()) {

            ArrayNode newArr = JsonNodeFactory.instance.arrayNode();

            for (JsonNode n : node) {
                newArr.add(toFlatNode(n, metaField));
            }

            node = newArr;
        }

        return node;
    }

    private void register(AttProcessor processor) {
        attProcessors.put(processor.getType(), processor);
    }
}
