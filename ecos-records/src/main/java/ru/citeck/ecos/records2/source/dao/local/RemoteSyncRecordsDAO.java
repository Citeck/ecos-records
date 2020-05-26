package ru.citeck.ecos.records2.source.dao.local;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.meta.AttributesSchema;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.RecordElements;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.SortBy;
import ru.citeck.ecos.records2.resolver.LocalRecordsResolver;
import ru.citeck.ecos.records2.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records2.source.dao.local.job.Job;
import ru.citeck.ecos.records2.source.dao.local.job.JobsProvider;
import ru.citeck.ecos.records2.source.dao.local.job.PeriodicJob;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class RemoteSyncRecordsDAO<T> extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<Object>,
               LocalRecordsMetaDAO<Object>,
               LocalRecordsQueryDAO,
               JobsProvider {

    private static final List<String> SUPPORTED_LANGUAGES = Collections.singletonList(
        PredicateService.LANGUAGE_PREDICATE
    );
    private static final String MODIFIED_ATT_KEY = "__sync_modified_att";

    private final Map<RecordRef, T> records = new ConcurrentHashMap<>();

    private final Class<T> model;
    private AttributesSchema schema;

    private final CompletableFuture<Boolean> firstSyncFuture = new CompletableFuture<>();
    private RemoteRecordsResolver remoteRecordsResolver;
    private LocalRecordsResolver localRecordsResolver;

    private Instant currentSyncDate = Instant.ofEpochMilli(0);

    public RemoteSyncRecordsDAO(String sourceId, Class<T> model) {
        setId(sourceId);
        this.model = model;
    }

    public Collection<T> getRecords() {
        return records.values();
    }

    @Override
    public RecordsQueryResult<RecordRef> queryLocalRecords(RecordsQuery query) {

        waitUntilSyncCompleted();

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

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(RecordsQuery query, MetaField field) {
        return new RecordsQueryResult<>(queryLocalRecords(query), ref -> records.get(toGlobalRef(ref)));
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        waitUntilSyncCompleted();
        return records.stream().map(this::toGlobalRef).map(this.records::get).collect(Collectors.toList());
    }

    public T getRecord(RecordRef recordRef) {
        waitUntilSyncCompleted();
        return records.get(recordRef);
    }

    private RecordRef toGlobalRef(RecordRef ref) {
        return RecordRef.valueOf(getId() + "@" + ref.getId());
    }

    private void waitUntilSyncCompleted() {
        try {
            if (!firstSyncFuture.isDone()) {
                serviceFactory.initJobs(null);
                log.warn("!!! Current thread will be blocked until data will be synchronized. "
                       + "SourceId: " + getId() + ". Timeout: 5min !!!");
            }
            firstSyncFuture.get(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            ExceptionUtils.throwException(e);
        }
    }

    /**
     * Update sync.
     * @return true if updating is not completed
     */
    private boolean update() {

        if (this.remoteRecordsResolver == null) {
            log.debug("Remote records resolver is not ready yet");
            return false;
        }

        Predicate predicate = Predicates.gt(RecordConstants.ATT_MODIFIED, currentSyncDate);

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(getId());
        query.setQuery(predicate);
        query.addSort(new SortBy(RecordConstants.ATT_MODIFIED, true));
        query.setMaxItems(50);
        query.setLanguage(PredicateService.LANGUAGE_PREDICATE);

        RecordsQueryResult<RecordMeta> result;
        try {
            if (getId().contains("/")) {
                result = remoteRecordsResolver.queryRecords(query, schema.getSchema());
            } else {
                result = localRecordsResolver.queryRecords(query, schema.getSchema());
            }
        } catch (Exception e) {
            // target DAO is not ready yet
            return false;
        }

        if (!result.getErrors().isEmpty()) {
            log.warn("Update failed: there are errors in query result. Result: " + result);
            return false;
        }

        Instant lastModified = currentSyncDate;
        List<RecordMeta> flatMeta = recordsMetaService.convertMetaResult(result.getRecords(), schema, true);

        for (RecordMeta meta : flatMeta) {

            String modifiedStr = meta.get(MODIFIED_ATT_KEY).asText();
            if (StringUtils.isBlank(modifiedStr)) {
                return false;
            }
            Instant modified = Instant.parse(modifiedStr);
            T instance = recordsMetaService.instantiateMeta(model, meta);

            records.put(meta.getId(), instance);

            if (modified.isAfter(lastModified)) {
                lastModified = modified;
            }
        }

        this.currentSyncDate = lastModified;

        if (result.getRecords().isEmpty() && !records.isEmpty()) {
            if (!firstSyncFuture.isDone()) {
                firstSyncFuture.complete(true);
            }
            return false;
        }
        return true;
    }

    @NotNull
    @Override
    public List<Job> getJobs() {
        return Collections.singletonList(new PeriodicJob() {

            @Override
            public long getPeriod() {
                return 10_000;
            }

            @Override
            public long getInitDelay() {
                return 2000;
            }

            @Override
            public boolean execute() {
                return update();
            }
        });
    }

    @Override
    public void setRecordsServiceFactory(RecordsServiceFactory serviceFactory) {

        super.setRecordsServiceFactory(serviceFactory);

        this.remoteRecordsResolver = serviceFactory.getRemoteRecordsResolver();
        this.localRecordsResolver = serviceFactory.getLocalRecordsResolver();

        if (this.getId().contains("/") && this.remoteRecordsResolver == null) {
            throw new IllegalStateException("Sync records DAO can't work "
                + "without RemoteRecordsResolver. SourceId: " + getId());
        }
        Map<String, String> attributes = new HashMap<>(recordsMetaService.getAttributes(model));
        attributes.put(MODIFIED_ATT_KEY, RecordConstants.ATT_MODIFIED);
        schema = recordsMetaService.createSchema(attributes);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }
}
