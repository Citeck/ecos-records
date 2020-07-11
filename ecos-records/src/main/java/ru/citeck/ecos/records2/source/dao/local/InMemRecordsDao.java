package ru.citeck.ecos.records2.source.dao.local;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.EmptyValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.RecordElements;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemRecordsDao<T> extends LocalRecordsDao
                                implements LocalRecordsQueryWithMetaDao<Object>,
                                           LocalRecordsMetaDao<Object>,
                                           LocalRecordsQueryDao {

    private static final List<String> SUPPORTED_LANGUAGES = Collections.singletonList(
        PredicateService.LANGUAGE_PREDICATE
    );

    private final Map<RecordRef, T> records = new ConcurrentHashMap<>();

    public InMemRecordsDao(String sourceId) {
        setId(sourceId);
    }

    public Map<RecordRef, T> getRecords() {
        return records;
    }

    public void setRecords(Map<RecordRef, T> values) {
        values.forEach(this::setRecord);
    }

    public void setRecord(RecordRef recordRef, T value) {
        this.records.put(toGlobalRef(recordRef), value);
    }

    public Optional<T> getRecord(RecordRef recordRef) {
        return Optional.ofNullable(records.get(toGlobalRef(recordRef)));
    }

    @NotNull
    @Override
    public RecordsQueryResult<RecordRef> queryLocalRecords(@NotNull RecordsQuery query) {

        Predicate predicate = query.getQuery(Predicate.class);

        RecordsQueryResult<RecordRef> result = new RecordsQueryResult<>();

        result.setRecords(predicateService.filter(
            new RecordElements(recordsService, new ArrayList<>(records.keySet())),
            predicate
            ).stream()
                .map(ref -> RecordRef.valueOf(ref.getRecordRef().getId()))
                .collect(Collectors.toList())
        );

        return result;
    }

    @NotNull
    @Override
    public RecordsQueryResult<Object> queryLocalRecords(@NotNull RecordsQuery query, @NotNull MetaField field) {
        return new RecordsQueryResult<>(queryLocalRecords(query), ref -> records.get(toGlobalRef(ref)));
    }

    @NotNull
    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
        @SuppressWarnings("unchecked")
        T empty = (T) EmptyValue.INSTANCE;
        return records.stream()
            .map(this::toGlobalRef)
            .map(ref -> this.records.getOrDefault(ref, empty))
            .collect(Collectors.toList());
    }

    private RecordRef toGlobalRef(RecordRef ref) {
        if (ref.getSourceId().isEmpty()) {
            return RecordRef.valueOf(getId() + "@" + ref.getId());
        } else {
            return ref;
        }
    }

    @NotNull
    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }
}
