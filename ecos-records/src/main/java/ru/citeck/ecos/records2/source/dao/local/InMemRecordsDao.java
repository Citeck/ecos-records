package ru.citeck.ecos.records2.source.dao.local;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records2.predicate.comparator.DefaultValueComparator;
import ru.citeck.ecos.records2.predicate.comparator.ValueComparator;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.atts.schema.ScalarType;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.atts.RecordAttsService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.RecordElements;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.SupportsQueryLanguages;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
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
    protected ValueComparator comparator = DefaultValueComparator.INSTANCE;

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

        List<Map.Entry<String, T>> recordsList = new ArrayList<>(records.entrySet());
        if (!query.getSortBy().isEmpty()) {

            List<String> attsToLoadAndSort = query.getSortBy()
                .stream()
                .map(it -> it.getAttribute() + ScalarType.RAW.getSchema())
                .collect(Collectors.toList());

            List<RecordAtts> allRecordsAtts = recordsService.getAtts(recordsList.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()), attsToLoadAndSort);
            Map<String, RecordAtts> attsById = new HashMap<>();

            for (int i = 0; i < allRecordsAtts.size(); i++) {
                attsById.put(recordsList.get(i).getKey(), allRecordsAtts.get(i));
            }

            recordsList = recordsList.stream().sorted((entry0, entry1) -> {

                RecordAtts atts0 = attsById.get(entry0.getKey());
                RecordAtts atts1 = attsById.get(entry1.getKey());

                int idx = 0;
                for (String att : attsToLoadAndSort) {

                    boolean ascending = query.getSortBy().get(idx++).getAscending();

                    DataValue value0 = atts0.getAtt(att);
                    DataValue value1 = atts1.getAtt(att);

                    int compareRes;
                    if (comparator.isGreaterThan(value0, value1, false)) {
                        compareRes = ascending ? 1 : -1;
                    } else if (comparator.isLessThan(value0, value1, false)) {
                        compareRes = ascending ? -1 : 1;
                    } else {
                        compareRes = 0;
                    }
                    if (compareRes != 0) {
                        return compareRes;
                    }
                }
                return 0;
            }).collect(Collectors.toList());
        }

        RecordElements elements = new RecordElements(recordsServiceV0, new ArrayList<>(recordsList
            .stream()
            .map(r -> RecordRef.create(getId(), r.getKey()))
            .collect(Collectors.toList())
        ));
        int maxItems = query.getPage().getMaxItems();
        if (maxItems < 0) {
            maxItems = Integer.MAX_VALUE;
        }
        result.setRecords(predicateService.filter(elements, predicate)
            .stream()
            .skip(query.getPage().getSkipCount())
            .limit(maxItems)
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
