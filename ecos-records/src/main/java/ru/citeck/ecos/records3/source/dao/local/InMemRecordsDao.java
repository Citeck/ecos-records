package ru.citeck.ecos.records3.source.dao.local;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.EmptyValue;
import ru.citeck.ecos.records3.record.operation.meta.RecordAttsService;
import ru.citeck.ecos.records3.predicate.PredicateService;
import ru.citeck.ecos.records3.predicate.RecordElements;
import ru.citeck.ecos.records3.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemRecordsDao<T> /*extends AbstractRecordsDao
                                implements RecordsMetaDao,
                                           RecordsQueryDao,
                                           ServiceFactoryAware */{

    private static final List<String> SUPPORTED_LANGUAGES = Collections.singletonList(
        PredicateService.LANGUAGE_PREDICATE
    );

    private final Map<RecordRef, T> records = new ConcurrentHashMap<>();
    protected PredicateService predicateService;
    protected RecordsService recordsService;
    protected RecordsServiceFactory serviceFactory;
    protected RecordAttsService recordsMetaService;

    public InMemRecordsDao(String sourceId) {
        //setId(sourceId);
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
    //@Override
    public RecordsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {

        Predicate predicate = query.getQuery(Predicate.class);

        RecordsQueryRes<RecordRef> result = new RecordsQueryRes<>();

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
    //@Override
    public List<?> getAtts(@NotNull List<RecordRef> records) {
        return records.stream()
            .map(this::toGlobalRef)
            .map(ref -> {
                Object res = this.records.get(ref);
                if (res == null) {
                    res = EmptyValue.INSTANCE;
                }
                return res;
            })
            .collect(Collectors.toList());
    }

    private RecordRef toGlobalRef(RecordRef ref) {
        if (ref.getSourceId().isEmpty()) {
            //return RecordRef.valueOf(getId() + "@" + ref.getId());
        } else {
            return ref;
        }
        return null;
    }

    @NotNull
    //@Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    //@Override
    public void setRecordsServiceFactory(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
        this.predicateService = serviceFactory.getPredicateService();
        this.recordsService = serviceFactory.getRecordsService();
        this.recordsMetaService = serviceFactory.getRecordsMetaService();
    }
}
