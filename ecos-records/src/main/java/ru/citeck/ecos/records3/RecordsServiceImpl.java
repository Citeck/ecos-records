package ru.citeck.ecos.records3;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.RecordAtts;
import ru.citeck.ecos.records3.record.op.delete.DelStatus;
import ru.citeck.ecos.records3.record.op.atts.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.op.atts.schema.read.AttReadException;
import ru.citeck.ecos.records3.record.op.atts.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.schema.write.AttSchemaWriter;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.op.query.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.RecordsQueryRes;
import ru.citeck.ecos.records3.record.resolver.LocalRemoteResolver;
import ru.citeck.ecos.records3.record.dao.RecordsDao;
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class RecordsServiceImpl extends AbstractRecordsService {

    private final LocalRemoteResolver recordsResolver;
    private final AttSchemaReader attSchemaReader;
    private final DtoSchemaReader dtoAttsSchemaReader;
    private final AttSchemaWriter attSchemaWriter;

    public RecordsServiceImpl(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
        dtoAttsSchemaReader = serviceFactory.getDtoSchemaReader();
        recordsResolver = serviceFactory.getRecordsResolver();
        attSchemaReader = serviceFactory.getAttSchemaReader();
        attSchemaWriter = serviceFactory.getAttSchemaWriter();
    }

    /* QUERY */

    @NotNull
    @Override
    public RecordsQueryRes<RecordRef> query(@NotNull RecordsQuery query) {
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
    public <T> RecordsQueryRes<T> query(@NotNull RecordsQuery query, @NotNull Class<T> metaClass) {

        List<SchemaRootAtt> attributes = dtoAttsSchemaReader.read(metaClass);
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Meta class doesn't has any fields with setter. Class: " + metaClass);
        }
        RecordsQueryRes<RecordAtts> meta = query(query, attSchemaWriter.writeToMap(attributes));

        return new RecordsQueryRes<>(meta, m -> dtoAttsSchemaReader.instantiate(metaClass, m.getAttributes()));
    }

    @NotNull
    @Override
    public RecordsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                             @NotNull Map<String, String> attributes,
                                             boolean rawAtts) {

        RecordsQueryRes<RecordAtts> result = handleRecordsQuery(() ->
            recordsResolver.query(query, attributes, rawAtts));

        if (result == null) {
            result = new RecordsQueryRes<>();
        }
        return result;
    }

    /* ATTRIBUTES */

    @NotNull
    @Override
    public List<RecordAtts> getAtts(@NotNull Collection<?> records,
                                    @NotNull Map<String, String> attributes,
                                    boolean rawAtts) {

        return handleRecordsListRead(() ->
            recordsResolver.getAtts(new ArrayList<>(records), attributes, rawAtts));
    }

    @NotNull
    @Override
    public <T> List<T> getAtts(@NotNull Collection<?> records, @NotNull Class<T> metaClass) {

        List<SchemaRootAtt> attributes = dtoAttsSchemaReader.read(metaClass);
        if (attributes.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        List<RecordAtts> meta = getAtts(records, attSchemaWriter.writeToMap(attributes));

        return meta.stream()
            .map(m -> dtoAttsSchemaReader.instantiate(metaClass, m.getAttributes()))
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

                    SchemaAtt parsedAtt = attSchemaReader.readRoot(name).getAttribute();
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
    public List<DelStatus> delete(@NotNull List<RecordRef> records) {
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
            result = RequestContext.doWithCtx(serviceFactory, ctx -> supplier.get());
        } catch (Throwable e) {
            log.error("Records resolving error", e);
            result = new RecordsQueryRes<>();
        }

        return result;
    }

    private <T> List<T> handleRecordsListRead(Supplier<List<T>> impl) {

        List<T> result;

        try {
            result = RequestContext.doWithCtx(serviceFactory, ctx -> impl.get());
        } catch (Throwable e) {
            log.error("Records resolving error", e);
            result = Collections.emptyList();
        }

        return result;
    }

    @Nullable
    @Override
    public RecordsDaoInfo getSourceInfo(String sourceId) {
        if (sourceId == null) {
            return null;
        }
        return recordsResolver.getSourceInfo(sourceId);
    }

    @NotNull
    @Override
    public List<RecordsDaoInfo> getSourcesInfo() {
        return recordsResolver.getSourceInfo();
    }

    @Override
    public void register(@NotNull RecordsDao recordsSource) {

        String id = recordsSource.getId();
        if (id == null) {
            throw new IllegalArgumentException("id is a mandatory parameter for RecordsDao");
        }
        register(id, recordsSource);
    }

    @Override
    public void register(@NotNull String sourceId, @NotNull RecordsDao recordsSource) {
        recordsResolver.register(sourceId, recordsSource);
    }
}
