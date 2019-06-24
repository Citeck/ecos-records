package ru.citeck.ecos.records2;

import ru.citeck.ecos.records2.request.query.RecordsQuery;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractRecordsService implements RecordsService {

    /* QUERY */

    @Override
    public Optional<RecordRef> queryRecord(RecordsQuery query) {
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
}
