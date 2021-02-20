package ru.citeck.ecos.records2.meta;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.*;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Deprecated
public class RecordsMetaServiceImpl implements RecordsMetaService {

    private final AttributesMetaResolver attributesMetaResolver;
    private final DtoSchemaReader dtoSchemaReader;
    private final AttSchemaWriter attSchemaWriter;

    public RecordsMetaServiceImpl(RecordsServiceFactory serviceFactory) {
        dtoSchemaReader = serviceFactory.getDtoSchemaReader();
        attributesMetaResolver = serviceFactory.getAttributesMetaResolver();
        attSchemaWriter = serviceFactory.getAttSchemaWriter();
    }

    @Override
    public List<RecordAtts> convertMetaResult(List<? extends RecordAtts> meta, AttributesSchema schema, boolean flat) {
        return meta.stream()
                   .map(m -> convertMetaResult(m, schema, flat))
                   .collect(Collectors.toList());
    }

    @Override
    public RecordAtts convertMetaResult(RecordAtts meta, AttributesSchema schema, boolean flat) {

        ObjectData attributes = meta.getAtts();
        ObjectData resultAttributes = ObjectData.create();
        Map<String, AttSchemaInfo> attsInfo = schema.getAttsInfo();

        Iterator<String> fieldsIt = attributes.fieldNames();
        while (fieldsIt.hasNext()) {
            processAttribute(fieldsIt.next(), attsInfo, attributes, resultAttributes, flat);
        }

        RecordAtts recordMeta = new RecordAtts(meta.getId());
        recordMeta.setAtts(resultAttributes);

        return recordMeta;
    }

    private void processAttribute(String key,
                                  Map<String, AttSchemaInfo> attsInfo,
                                  ObjectData attributes,
                                  ObjectData resultAttributes,
                                  boolean flat) {

        AttSchemaInfo attInfo = attsInfo.get(key);
        if (attInfo == null) {
            return;
        }
        String resultKey = attInfo.getOriginalKey();
        if (StringUtils.isBlank(attInfo.getOriginalKey())) {
            return;
        }

        DataValue value = attributes.get(key);

        if (resultKey.equals(".json") || !flat) {

            resultAttributes.set(resultKey, value);

        } else {

            resultAttributes.set(resultKey, toFlatNode(value.getValue()));
        }
    }

    @Override
    public Map<String, String> getAttributes(Class<?> metaClass) {
        List<SchemaAtt> atts = dtoSchemaReader.read(metaClass);
        return attSchemaWriter.writeToMap(atts);
    }

    @Override
    public <T> T instantiateMeta(Class<T> metaClass, RecordMeta flatMeta) {
        return dtoSchemaReader.instantiate(metaClass, flatMeta.getAttributes());
    }

    @Override
    public AttributesSchema createSchema(Map<String, String> attributes) {
        return attributesMetaResolver.createSchema(attributes);
    }

    private JsonNode toFlatNode(JsonNode input) {

        if (input == null || input.isMissingNode() || input.isNull()) {
            return NullNode.getInstance();
        }

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
