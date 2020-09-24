package ru.citeck.ecos.records3.record.operation.meta;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.schema.AttsSchema;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.DtoSchemaResolver;
import ru.citeck.ecos.records3.record.error.ErrorUtils;
import ru.citeck.ecos.records3.request.result.RecordsResult;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RecordsMetaServiceImpl implements RecordsMetaService {

    private final AttributesMetaResolver attributesMetaResolver;
    private final DtoSchemaResolver dtoMetaResolver;

    public RecordsMetaServiceImpl(RecordsServiceFactory serviceFactory) {
        dtoMetaResolver = serviceFactory.getDtoMetaResolver();
        attributesMetaResolver = serviceFactory.getAttributesMetaResolver();
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(List<?> records, String schema) {
        if (StringUtils.isBlank(schema)) {
            schema = "id";
        }
        //return new RecordsResult<>(graphQLService.getMeta(records, schema));
        return null;
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

        //AttsSchemaImpl schema = createSchema(attributes);
        //RecordsResult<RecordMeta> meta = getMeta(records, schema.getGqlSchema());
        //meta.setRecords(convertMetaResult(meta.getRecords(), schema, true));

        //return meta;
        return null;
    }

    @Override
    public RecordMeta convertMetaResult(RecordMeta meta, AttsSchema schema, boolean flat) {
        return null;
    }

    @Override
    public List<RecordMeta> convertMetaResult(List<RecordMeta> meta, AttsSchema schema, boolean flat) {
        return meta.stream()
                   .map(m -> convertMetaResult(m, schema, flat))
                   .collect(Collectors.toList());
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
    public AttsSchema createSchema(Map<String, String> attributes) {
        //return attributesMetaResolver.createAttsSchema(attributes);
        return null;
    }
}
