package ru.citeck.ecos.records2.source.dao.local;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.op.atts.service.RecordAttsService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.RecordElements;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.op.query.dao.SupportsQueryLanguages;
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InMemRecordsDao<T> extends AbstractRecordsDao
                                implements RecordsAttsDao,
                                           RecordsQueryDao,
                                           ServiceFactoryAware, SupportsQueryLanguages {

    private static final List<String> SUPPORTED_LANGUAGES = Collections.singletonList(
        PredicateService.LANGUAGE_PREDICATE
    );

    private final String sourceId;

    private final Map<String, T> records = new ConcurrentHashMap<>();
    private final List<Consumer<T>> onChangeListeners = new CopyOnWriteArrayList<>();

    protected PredicateService predicateService;
    protected RecordsService recordsService;
    protected RecordsServiceFactory serviceFactory;
    protected RecordAttsService recordsMetaService;
    protected ru.citeck.ecos.records2.RecordsService recordsServiceV0;

    @NotNull
    @Override
    public String getId() {
        return sourceId;
    }

    public InMemRecordsDao(String sourceId) {
        this.sourceId = sourceId;
    }

    public Map<String, T> getRecords() {
        return records;
    }

    public void setRecords(Map<String, T> values) {
        values.forEach(this::setRecord);
    }

    public void setRecord(String recordId, T value) {
        this.records.put(recordId, value);
        onChangeListeners.forEach(it -> it.accept(value));
    }

    public Optional<T> getRecord(String recordId) {
        return Optional.ofNullable(records.get(recordId));
    }

    @NotNull
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {

        Predicate predicate = query.getQuery(Predicate.class);

        RecsQueryRes<RecordRef> result = new RecsQueryRes<>();

        result.setRecords(predicateService.filter(
            new RecordElements(recordsServiceV0, new ArrayList<>(records.keySet()
                .stream()
                .map(r -> RecordRef.create(getId(), r))
                .collect(Collectors.toList())
            )),
            predicate
            ).stream()
                .map(RecordElement::getRecordRef)
                .collect(Collectors.toList())
        );

        return result;
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return records.stream()
            .map(ref -> {
                Object res = this.records.get(ref);
                if (res == null) {
                    res = EmptyAttValue.INSTANCE;
                }
                return res;
            })
            .collect(Collectors.toList());
    }

    public void addOnChangeListener(Consumer<T> listener) {
        this.onChangeListeners.add(listener);
    }

    @NotNull
    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public void setRecordsServiceFactory(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
        this.predicateService = serviceFactory.getPredicateService();
        this.recordsService = serviceFactory.getRecordsServiceV1();
        this.recordsMetaService = serviceFactory.getRecordsAttsService();
        this.recordsServiceV0 = serviceFactory.getRecordsService();
    }
}
