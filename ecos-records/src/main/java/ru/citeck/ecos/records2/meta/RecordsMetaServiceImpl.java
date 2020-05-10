package ru.citeck.ecos.records2.meta;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RecordsMetaServiceImpl implements RecordsMetaService {

    private RecordsMetaGql graphQLService;
    private AttributesMetaResolver attributesMetaResolver;
    private DtoMetaResolver dtoMetaResolver;

    public RecordsMetaServiceImpl(RecordsServiceFactory serviceFactory) {
        graphQLService = serviceFactory.getRecordsMetaGql();
        dtoMetaResolver = serviceFactory.getDtoMetaResolver();
        attributesMetaResolver = serviceFactory.getAttributesMetaResolver();
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
        RecordsResult<RecordMeta> meta = getMeta(records, schema.getSchema());
        meta.setRecords(convertMetaResult(meta.getRecords(), schema, true));

        return meta;
    }

    @Override
    public List<RecordMeta> convertMetaResult(List<RecordMeta> meta, AttributesSchema schema, boolean flat) {
        return meta.stream()
                   .map(m -> convertMetaResult(m, schema, flat))
                   .collect(Collectors.toList());
    }

    private RecordMeta convertMetaResult(RecordMeta meta, AttributesSchema schema, boolean flat) {

        ObjectData attributes = meta.getAttributes();
        ObjectData resultAttributes = ObjectData.create();
        Map<String, String> keysMapping = schema.getKeysMapping();

        attributes.forEach((key, value) -> {

            String resultKey = keysMapping.get(key);
            if (resultKey != null) {
                if (resultKey.equals(".json") || !flat) {
                    resultAttributes.set(resultKey, value);
                } else {
                    resultAttributes.set(resultKey, toFlatNode(Json.getMapper().toJson(value)));
                }
            }
        });

        RecordMeta recordMeta = new RecordMeta(meta.getId());
        recordMeta.setAttributes(resultAttributes);

        return recordMeta;
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

    private JsonNode toFlatNode(JsonNode input) {

        JsonNode node = input;

        if (node.isObject() && node.size() > 1) {

            ObjectNode objNode = JsonNodeFactory.instance.objectNode();
            final JsonNode finalNode = node;

            node.fieldNames().forEachRemaining(name ->
                objNode.set(name, toFlatNode(finalNode.get(name)))
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
}
