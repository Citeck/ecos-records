package ru.citeck.ecos.records2;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractRecordsService implements RecordsService {

    protected RecordsServiceFactory serviceFactory;

    AbstractRecordsService(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    /* QUERY */

    @Override
    public Optional<EntityRef> queryRecord(RecordsQuery query) {
        return queryRecords(query).getRecords().stream().findFirst();
    }

    @Override
    public <T> Optional<T> queryRecord(RecordsQuery query, Class<T> metaClass) {
        return queryRecords(query, metaClass).getRecords().stream().findFirst();
    }

    @Override
    public Optional<RecordMeta> queryRecord(RecordsQuery query, Map<String, String> attributes) {
        return queryRecords(query, attributes).getRecords().stream().findFirst();
    }

    @Override
    public Optional<RecordMeta> queryRecord(RecordsQuery query, Collection<String> attributes) {
        return queryRecords(query, attributes).getRecords().stream().findFirst();
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Collection<String> attributes) {
        return queryRecords(query, toAttributesMap(attributes));
    }

    /* ATTRIBUTES */

    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<EntityRef> records,
                                                   Collection<String> attributes) {

        return getAttributes(records, toAttributesMap(attributes));
    }

    @Override
    public RecordMeta getAttributes(EntityRef record, Collection<String> attributes) {
        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordMeta getAttributes(EntityRef record, Map<String, String> attributes) {
        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordMeta getRawAttributes(EntityRef record, Map<String, String> attributes) {
        return extractOne(getRawAttributes(Collections.singletonList(record), attributes), record);
    }

    /* MUTATE */

    @Override
    public RecordMeta mutate(RecordMeta meta) {

        RecordsMutation mutation = new RecordsMutation();
        mutation.addRecord(meta);
        RecordsMutResult result = this.mutate(mutation);

        ErrorUtils.logErrors(result);

        return result.getRecords()
            .stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Record mutation failed. Meta: " + meta));
    }

    /* UTILS */

    private RecordMeta extractOne(RecordsResult<RecordMeta> values, EntityRef record) {

        if (values.getRecords().isEmpty()) {
            return new RecordMeta(record);
        }
        RecordMeta meta;
        if (values.getRecords().size() == 1) {
            meta = values.getRecords().get(0);
            if (!record.equals(meta.getId())) {
                meta = new RecordMeta(meta, record);
            }
            return meta;
        }

        meta = values.getRecords()
                     .stream()
                     .filter(r -> record.equals(r.getId()))
                     .findFirst()
                     .orElse(null);

        if (meta == null && values.getRecords().size() > 0) {
            log.warn("Records is not empty but '" + record + "' is not found. Records: "
                + values.getRecords()
                        .stream()
                        .map(m -> "'" + m.getId() + "'")
                        .collect(Collectors.joining(", ")));
        }
        if (meta == null) {
            meta = new RecordMeta(record);
        }
        return meta;
    }

    private Map<String, String> toAttributesMap(Collection<String> attributes) {
        Map<String, String> attributesMap = new HashMap<>();
        for (String attribute : attributes) {
            attributesMap.put(attribute, attribute);
        }
        return attributesMap;
    }
}
