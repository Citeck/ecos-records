package ru.citeck.ecos.records3;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.record.operation.delete.DelStatus;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttReadException;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.DtoSchemaResolver;
import ru.citeck.ecos.records3.record.operation.query.QueryContext;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.record.resolver.RecordsDaoRegistry;
import ru.citeck.ecos.records3.record.resolver.RecordsResolver;
import ru.citeck.ecos.records3.source.dao.RecordsDao;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class RecordsServiceImpl extends AbstractRecordsService {

    private final RecordsResolver recordsResolver;
    private final AttSchemaReader attSchemaReader;
    private final DtoSchemaResolver dtoAttributesResolver;

    public RecordsServiceImpl(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
        dtoAttributesResolver = serviceFactory.getDtoMetaResolver();
        recordsResolver = serviceFactory.getRecordsResolver();
        attSchemaReader = serviceFactory.getAttSchemaReader();
    }

    /* QUERY */

    @NotNull
    @Override
    public RecordsQueryRes<RecordRef> query(RecordsQuery query) {
        return handleRecordsQuery(() -> {
            RecordsQueryRes<RecordAtts> metaResult = recordsResolver.query(query,
                Collections.emptyMap(), true);
            if (metaResult == null) {
                metaResult = new RecordsQueryRes<>();
            }
            return new RecordsQueryRes<>(metaResult, RecordAtts::getId);
        });
    }

    @NotNull
    @Override
    public <T> RecordsQueryRes<T> query(RecordsQuery query, Class<T> metaClass) {

        Map<String, String> attributes = dtoAttributesResolver.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Meta class doesn't has any fields with setter. Class: " + metaClass);
        }
        RecordsQueryRes<RecordAtts> meta = query(query, attributes);

        return new RecordsQueryRes<>(meta, m -> dtoAttributesResolver.instantiateMeta(metaClass, m.getAttributes()));
    }

    @NotNull
    @Override
    public RecordsQueryRes<RecordAtts> query(RecordsQuery query,
                                             Map<String, String> attributes,
                                             boolean rawAtts) {

        RecordsQueryRes<RecordAtts> result = handleRecordsQuery(() ->
            recordsResolver.query(query, attributes, true));

        if (result == null) {
            result = new RecordsQueryRes<>();
        }
        return result;
    }

    /* ATTRIBUTES */

    @NotNull
    @Override
    public List<RecordAtts> getAtts(Collection<RecordRef> records,
                                    Map<String, String> attributes,
                                    boolean rawAtts) {

        if (attributes.isEmpty()) {
            return records.stream().map(RecordAtts::new).collect(Collectors.toList());
        }

        List<RecordAtts> meta = handleRecordsListRead(() ->
            recordsResolver.getAtts(new ArrayList<>(records), attributes, rawAtts));

        return meta != null ? meta : Collections.emptyList();
    }

    @NotNull
    @Override
    public <T> T getAtts(@NotNull RecordRef recordRef, @NotNull Class<T> metaClass) {

        List<T> meta = getAtts(Collections.singletonList(recordRef), metaClass);
        if (meta.size() == 0) {
            throw new IllegalStateException("Can't get record metadata. Ref: " + recordRef + " Result: " + meta);
        }
        return meta.get(0);
    }

    @NotNull
    @Override
    public <T> List<T> getAtts(@NotNull Collection<RecordRef> records, @NotNull Class<T> metaClass) {

        Map<String, String> attributes = dtoAttributesResolver.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        List<RecordAtts> meta = getAtts(records, attributes);

        return meta.stream()
            .map(m -> dtoAttributesResolver.instantiateMeta(metaClass, m.getAttributes()))
            .collect(Collectors.toList());
    }

    /* MUTATE */

    @NotNull
    @Override
    public List<RecordRef> mutate(List<RecordAtts> records) {

        Map<String, RecordRef> aliasToRecordRef = new HashMap<>();
        List<RecordRef> result = new ArrayList<>();

        for (int i = records.size() - 1; i >= 0; i--) {

            RecordAtts record = records.get(i);

            ObjectData attributes = ObjectData.create();

            record.forEach((name, value) -> {

                try {

                    SchemaAtt parsedAtt = attSchemaReader.read(name).getAttribute();
                    String scalarName = parsedAtt.getScalarName();

                    if (".assoc".equals(scalarName)) {
                        value = convertAssocValue(value, aliasToRecordRef);
                    }
                    attributes.set(parsedAtt.getName(), value);

                } catch (AttReadException e) {
                    log.error("Attribute read failed", e);
                }
            });

            record.setAttributes(attributes);

            List<RecordAtts> sourceMut = Collections.singletonList(record);
            List<RecordRef> recordMutResult = recordsResolver.mutate(sourceMut);
            if (recordMutResult == null) {
                recordMutResult = sourceMut.stream()
                    .map(RecordAtts::getId)
                    .collect(Collectors.toList());
            }

            for (int resIdx = recordMutResult.size() - 1; resIdx >= 0; resIdx--) {
                result.add(0, recordMutResult.get(resIdx));
            }

            for (RecordRef resultMeta : recordMutResult) {
                String alias = record.get(RecordConstants.ATT_ALIAS, "");
                if (StringUtils.isNotBlank(alias)) {
                    aliasToRecordRef.put(alias, resultMeta);
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

    @NotNull
    @Override
    public List<DelStatus> delete(List<RecordRef> records) {
        List<DelStatus> result = recordsResolver.delete(records);
        if (result == null) {
            result = new ArrayList<>(records.size());
            for (int i = 0; i < records.size(); i++) {
                result.add(DelStatus.OK);
            }
        }
        return result;
    }

    /* OTHER */

    private <T> RecordsQueryRes<T> handleRecordsQuery(Supplier<RecordsQueryRes<T>> supplier) {

        RecordsQueryRes<T> result;

        try {
            result = QueryContext.withContext(serviceFactory, supplier);
        } catch (Throwable e) {
            log.error("Records resolving error", e);
            result = new RecordsQueryRes<>();
        }

        return result;
    }

    private <T> List<T> handleRecordsListRead(Supplier<List<T>> impl) {

        List<T> result;

        try {
            result = QueryContext.withContext(serviceFactory, impl);
        } catch (Throwable e) {
            log.error("Records resolving error", e);
            result = Collections.emptyList();
        }

        return result;
    }

    @Nullable
    @Override
    public RecsSourceInfo getSourceInfo(String sourceId) {
        return recordsResolver.getSourceInfo(sourceId);
    }

    @NotNull
    @Override
    public List<RecsSourceInfo> getSourcesInfo() {
        return recordsResolver.getSourceInfo();
    }

    @Override
    public void register(RecordsDao recordsSource) {

        String id = recordsSource.getId();
        if (id == null) {
            throw new IllegalArgumentException("id is a mandatory parameter for RecordsDao");
        }
        register(id, recordsSource);
    }

    @Override
    public void register(String sourceId, RecordsDao recordsSource) {
        if (recordsResolver instanceof RecordsDaoRegistry) {
            ((RecordsDaoRegistry) recordsResolver).register(sourceId, recordsSource);
        } else {
            log.warn("Records resolver doesn't support source registration. "
                + "Source: " + sourceId + " " + recordsSource.getClass());
        }
    }
}
