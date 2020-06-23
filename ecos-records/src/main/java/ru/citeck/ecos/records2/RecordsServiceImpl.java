package ru.citeck.ecos.records2;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.meta.AttributesSchema;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.resolver.RecordsDaoRegistry;
import ru.citeck.ecos.records2.resolver.RecordsResolver;
import ru.citeck.ecos.records2.source.dao.RecordsDao;
import ru.citeck.ecos.records2.source.info.RecordsSourceInfo;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RecordsServiceImpl extends AbstractRecordsService {

    private static final Pattern ATT_PATTERN = Pattern.compile("^\\.atts?\\(n:\"([^\"]+)\"\\).+");

    private RecordsResolver recordsResolver;
    private RecordsMetaService recordsMetaService;

    public RecordsServiceImpl(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
        recordsResolver = serviceFactory.getRecordsResolver();
        recordsMetaService = serviceFactory.getRecordsMetaService();
    }

    /* QUERY */

    @Override
    public RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query) {
        return handleRecordsQuery(() -> {
            RecordsQueryResult<RecordMeta> metaResult = recordsResolver.queryRecords(query, "");
            return new RecordsQueryResult<>(metaResult, RecordMeta::getId);
        });
    }

    @Override
    public <T> RecordsQueryResult<T> queryRecords(RecordsQuery query, Class<T> metaClass) {

        Map<String, String> attributes = recordsMetaService.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Meta class doesn't has any fields with setter. Class: " + metaClass);
        }

        RecordsQueryResult<RecordMeta> meta = queryRecords(query, attributes);

        return new RecordsQueryResult<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes) {

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        RecordsQueryResult<RecordMeta> records = queryRecords(query, schema.getSchema());
        records.setRecords(recordsMetaService.convertMetaResult(records.getRecords(), schema, true));

        return records;
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {
        return handleRecordsQuery(() -> recordsResolver.queryRecords(query, schema));
    }

    /* ATTRIBUTES */

    @Override
    public DataValue getAtt(RecordRef record, String attribute) {
        return getAttribute(record, attribute);
    }

    @Override
    public DataValue getAttribute(RecordRef record, String attribute) {

        if (record == null) {
            return DataValue.create(null);
        }

        RecordsResult<RecordMeta> meta = getAttributes(Collections.singletonList(record),
                                                       Collections.singletonList(attribute));
        if (!meta.getRecords().isEmpty()) {
            return meta.getRecords().get(0).getAttribute(attribute);
        }
        return DataValue.create(null);
    }

    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records,
                                                   Map<String, String> attributes) {

        return getAttributesImpl(records, attributes, true);
    }

    @Override
    public RecordsResult<RecordMeta> getRawAttributes(Collection<RecordRef> records, Map<String, String> attributes) {
        return getAttributesImpl(records, attributes, false);
    }

    private RecordsResult<RecordMeta> getAttributesImpl(Collection<RecordRef> records,
                                                        Map<String, String> attributes,
                                                        boolean flatAttributes) {

        if (attributes.isEmpty()) {
            return new RecordsResult<>(new ArrayList<>(records), RecordMeta::new);
        }

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        RecordsResult<RecordMeta> meta = getMeta(records, schema.getSchema());
        meta.setRecords(recordsMetaService.convertMetaResult(meta.getRecords(), schema, flatAttributes));

        return meta;
    }

    /* META */

    @Override
    public <T> T getMeta(RecordRef recordRef, Class<T> metaClass) {

        if (recordRef == null) {
            return null;
        }

        RecordsResult<T> meta = getMeta(Collections.singletonList(recordRef), metaClass);
        if (meta.getRecords().size() == 0) {
            throw new IllegalStateException("Can't get record metadata. Ref: " + recordRef + " Result: " + meta);
        }
        return meta.getRecords().get(0);
    }

    @Override
    public <T> RecordsResult<T> getMeta(Collection<RecordRef> records, Class<T> metaClass) {

        Map<String, String> attributes = recordsMetaService.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        RecordsResult<RecordMeta> meta = getAttributes(records, attributes);

        return new RecordsResult<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {
        return handleRecordsRead(() -> recordsResolver.getMeta(records, schema), RecordsResult::new);
    }

    /* MODIFICATION */

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        Map<String, RecordRef> aliasToRecordRef = new HashMap<>();
        RecordsMutResult result = new RecordsMutResult();

        List<RecordMeta> records = mutation.getRecords();

        for (int i = records.size() - 1; i >= 0; i--) {

            RecordMeta record = records.get(i);

            ObjectData attributes = ObjectData.create();

            record.forEach((name, value) -> {

                String simpleName = name;

                if (name.charAt(0) != '.') {

                    int dotIdx = name.indexOf('.', 1);
                    int bracketIdx = name.indexOf('{');

                    int nameEndIdx = dotIdx;
                    if (dotIdx == -1 || bracketIdx != -1 && bracketIdx < dotIdx) {
                        nameEndIdx = bracketIdx;
                    }

                    if (nameEndIdx > 0) {
                        simpleName = name.substring(0, nameEndIdx);
                    } else {
                        int questionIdx = name.indexOf('?');
                        if (questionIdx > 0) {
                            simpleName = name.substring(0, questionIdx);
                        }
                    }

                } else {

                    Matcher matcher = ATT_PATTERN.matcher(name);
                    if (matcher.matches()) {
                        simpleName = matcher.group(1);
                    } else {
                        simpleName = null;
                    }
                }

                if (StringUtils.isNotBlank(simpleName)) {

                    if (name.endsWith("?assoc") || name.endsWith("{assoc}") || name.endsWith("{.assoc}")) {
                        value = convertAssocValue(value, aliasToRecordRef);
                    }

                    attributes.set(simpleName, value);
                }
            });

            record.setAttributes(attributes);

            RecordsMutation sourceMut = new RecordsMutation();
            sourceMut.setRecords(Collections.singletonList(record));
            RecordsMutResult recordMutResult = recordsResolver.mutate(sourceMut);

            if (i == 0) {
                result.merge(recordMutResult);
            }

            List<RecordMeta> resultRecords = recordMutResult.getRecords();
            for (RecordMeta resultMeta : resultRecords) {
                String alias = record.get(RecordConstants.ATT_ALIAS, "");
                if (StringUtils.isNotBlank(alias)) {
                    aliasToRecordRef.put(alias, resultMeta.getId());
                }
            }
        }

        return result;
    }

    private DataValue convertAssocValue(DataValue value, Map<String, RecordRef> mapping) {
        if (value.isTextual()) {
            String textValue = value.asText();
            if (mapping.containsKey(textValue)) {
                return DataValue.create(mapping.get(textValue).toString());
            }
        } else if (value.isArray()) {
            List<DataValue> convertedValue = new ArrayList<>();
            for (DataValue node : value) {
                convertedValue.add(convertAssocValue(node, mapping));
            }
            return DataValue.create(convertedValue);
        }
        return value;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        return recordsResolver.delete(deletion);
    }

    /* OTHER */

    private <T> RecordsQueryResult<T> handleRecordsQuery(Supplier<RecordsQueryResult<T>> supplier) {
        return handleRecordsRead(supplier, RecordsQueryResult::new);
    }

    private <T extends RecordsResult> T handleRecordsRead(Supplier<T> impl, Supplier<T> orElse) {

        T result;

        try {
            result = QueryContext.withContext(serviceFactory, impl);
        } catch (Throwable e) {
            log.error("Records resolving error", e);
            result = orElse.get();
            result.addError(ErrorUtils.convertException(e));
        }

        return result;
    }

    @Override
    public RecordsSourceInfo getSourceInfo(String sourceId) {
        return recordsResolver.getSourceInfo(sourceId);
    }

    @Override
    public List<RecordsSourceInfo> getSourcesInfo() {
        return recordsResolver.getSourceInfo();
    }

    @Override
    public void register(RecordsDao recordsSource) {

        String id = recordsSource.getId();
        if (id == null) {
            throw new IllegalArgumentException("id is a mandatory parameter for RecordsDao");
        }

        if (recordsResolver instanceof RecordsDaoRegistry) {
            ((RecordsDaoRegistry) recordsResolver).register(recordsSource);
        } else {
            log.warn("Records resolver doesn't support source registration. "
                     + "Source: " + id + " " + recordsSource.getClass());
        }
    }
}
