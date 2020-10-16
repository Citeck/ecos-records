package ru.citeck.ecos.records3;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.utils.AttUtils;

import java.util.*;

@Slf4j
public abstract class AbstractRecordsService implements RecordsService {

    protected RecordsServiceFactory serviceFactory;

    AbstractRecordsService(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    /* QUERY */

    @Nullable
    @Override
    public RecordRef queryOne(@NotNull RecordsQuery query) {
        return query(query)
            .getRecords()
            .stream()
            .findFirst()
            .orElse(null);
    }

    @Nullable
    @Override
    public <T> T queryOne(@NotNull RecordsQuery query, @NotNull Class<T> metaClass) {
        return query(query, metaClass)
            .getRecords()
            .stream()
            .findFirst()
            .orElse(null);
    }

    @Nullable
    @Override
    public RecordAtts queryOne(@NotNull RecordsQuery query, @NotNull Map<String, String> attributes) {
        return query(query, attributes)
            .getRecords()
            .stream()
            .findFirst()
            .orElse(null);
    }

    @Nullable
    @Override
    public RecordAtts queryOne(@NotNull RecordsQuery query, @NotNull Collection<String> attributes) {
        return query(query, attributes)
            .getRecords()
            .stream()
            .findFirst()
            .orElse(null);
    }

    @NotNull
    @Override
    public RecsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                          @NotNull Collection<String> attributes) {
        return query(query, AttUtils.toMap(attributes));
    }

    @NotNull
    @Override
    public RecsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                          @NotNull Map<String, String> attributes) {

        return query(query, attributes, false);
    }

    /* ATTRIBUTES */

    @NotNull
    @Override
    public DataValue getAtt(@Nullable Object record, @Nullable String attribute) {

        if (record == null || StringUtils.isBlank(attribute)) {
            return DataValue.NULL;
        }

        List<RecordAtts> meta = getAtts(Collections.singletonList(record),
                                        Collections.singletonList(attribute));

        return meta.get(0).getAttribute(attribute);
    }

    @NotNull
    @Override
    public <T> T getAtts(@Nullable Object record, @NotNull Class<T> metaClass) {
        if (record == null) {
            record = ObjectData.create();
        }
        return getAtts(Collections.singletonList(record), metaClass).get(0);
    }

    @NotNull
    @Override
    public List<RecordAtts> getAtts(@NotNull Collection<?> records,
                                    @NotNull Collection<String> attributes) {

        return getAtts(records, AttUtils.toMap(attributes), false);
    }

    @NotNull
    @Override
    public RecordAtts getAtts(@Nullable Object record,
                              @NotNull Collection<String> attributes) {
        if (record == null) {
            record = ObjectData.create();
        }
        return getAtts(Collections.singletonList(record), attributes).get(0);
    }

    @NotNull
    @Override
    public RecordAtts getAtts(@Nullable Object record,
                              @NotNull Map<String, String> attributes) {
        if (record == null) {
            record = ObjectData.create();
        }
        return getAtts(Collections.singletonList(record), attributes, false).get(0);
    }

    @NotNull
    @Override
    public List<RecordAtts> getAtts(@NotNull Collection<?> records,
                                    @NotNull Map<String, String> attributes) {

        return getAtts(records, attributes, false);
    }

    /* MUTATE */

    @NotNull
    @Override
    public RecordRef mutate(@Nullable Object record,
                            @Nullable String attribute,
                            @Nullable Object value) {

        if (record == null || StringUtils.isBlank(attribute)) {
            return record instanceof RecordRef ? (RecordRef) record : RecordRef.EMPTY;
        }
        return mutate(record, Collections.singletonMap(attribute, value));
    }

    @NotNull
    @Override
    public RecordRef mutate(Object record, @NotNull Map<String, Object> attributes) {
        return mutate(record, ObjectData.create(attributes));
    }

    @NotNull
    @Override
    public RecordRef mutate(Object record, @NotNull ObjectData attributes) {
        if (record instanceof RecordRef) {
            return mutate(new RecordAtts((RecordRef) record, attributes));
        }
        throw new RuntimeException("Mutation of custom objects is not supported yet");
    }

    @NotNull
    @Override
    public RecordRef mutate(@NotNull RecordAtts meta) {

        List<RecordAtts> records = Collections.singletonList(meta);
        List<RecordRef> recordRefs = this.mutate(records);

        if (recordRefs.size() != 1) {
            log.warn("Strange behaviour. Expected 1 record, but found " + recordRefs.size());
        }

        return recordRefs.get(0);
    }

    /* DELETE */

    @NotNull
    @Override
    public DelStatus delete(RecordRef record) {

        List<DelStatus> result = delete(Collections.singletonList(record));

        if (result.size() != 1) {
            log.warn("Strange behaviour. Expected 1 record, but found " + result.size());
        }

        return result.get(0);
    }
}
