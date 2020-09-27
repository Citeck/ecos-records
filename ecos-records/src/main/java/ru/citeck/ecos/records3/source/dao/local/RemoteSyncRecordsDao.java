package ru.citeck.ecos.records3.source.dao.local;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.record.operation.meta.schema.AttSchema;
import ru.citeck.ecos.records3.predicate.PredicateService;
import ru.citeck.ecos.records3.predicate.model.Predicate;
import ru.citeck.ecos.records3.predicate.model.Predicates;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.record.operation.query.dto.SortBy;
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolver;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records3.source.dao.local.job.Job;
import ru.citeck.ecos.records3.source.dao.local.job.JobsProvider;
import ru.citeck.ecos.records3.source.dao.local.job.PeriodicJob;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RemoteSyncRecordsDao<T> extends InMemRecordsDao<T> implements JobsProvider {

    private static final String MODIFIED_ATT_KEY = "__sync_modified_att";

    private final Class<T> model;
    private AttSchema schema;

    private final CompletableFuture<Boolean> firstSyncFuture = new CompletableFuture<>();
    private RemoteRecordsResolver remoteRecordsResolver;
    private LocalRecordsResolver localRecordsResolver;

    private Instant currentSyncDate = Instant.ofEpochMilli(0);

    public RemoteSyncRecordsDao(String sourceId, Class<T> model) {
        super(sourceId);
        this.model = model;
    }

    @NotNull
    @Override
    public RecordsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
        waitUntilSyncCompleted();
        return super.queryRecords(query);
    }

    @NotNull
    //@Override
    public List<?> getRecordsMeta(@NotNull List<RecordRef> records) {
        waitUntilSyncCompleted();
        //return super.getRecordsMeta(records);
        return null;
    }

    @Override
    public Optional<T> getRecord(RecordRef recordRef) {
        waitUntilSyncCompleted();
        return super.getRecord(recordRef);
    }

    private void waitUntilSyncCompleted() {
        try {
            if (!firstSyncFuture.isDone()) {
                serviceFactory.initJobs(null);
                /*log.warn("!!! Current thread will be blocked until data will be synchronized. "
                       + "SourceId: " + getId() + ". Timeout: 5min !!!");*/
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

        /*if (this.remoteRecordsResolver == null) {
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

        RecordsQueryRes<RecordAtts> result;
        try {
            if (getId().contains("/")) {
                result = remoteRecordsResolver.queryRecords(query, schema.getSourceAtts(), true);
            } else {
                result = localRecordsResolver.queryRecords(query, schema.getSourceAtts(), true);
            }
        } catch (Exception e) {
            // target DAO is not ready yet
            return false;
        }

        if (result == null || !result.getErrors().isEmpty()) {
            log.warn("Update failed: there are errors in query result. Result: " + result);
            return false;
        }

        Instant lastModified = currentSyncDate;
        List<RecordAtts> flatMeta = recordsMetaService.convertMetaResult(result.getRecords(), schema, true);

        for (RecordAtts meta : flatMeta) {

            String modifiedStr = meta.get(MODIFIED_ATT_KEY).asText();
            if (StringUtils.isBlank(modifiedStr)) {
                return false;
            }
            Instant modified = Instant.parse(modifiedStr);
            T instance = recordsMetaService.instantiateMeta(model, meta);

            setRecord(meta.getId(), instance);

            if (modified.isAfter(lastModified)) {
                lastModified = modified;
            }
        }

        this.currentSyncDate = lastModified;

        if (result.getRecords().isEmpty()) {
            if (!firstSyncFuture.isDone()) {
                firstSyncFuture.complete(true);
            }
            return false;
        }*/
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

        /*if (this.getId().contains("/") && this.remoteRecordsResolver == null) {
            throw new IllegalStateException("Sync records DAO can't work "
                + "without RemoteRecordsResolver. SourceId: " + getId());
        }
        Map<String, String> attributes = new HashMap<>(recordsMetaService.getAttributes(model));
        attributes.put(MODIFIED_ATT_KEY, RecordConstants.ATT_MODIFIED);
        schema = recordsMetaService.createSchema(attributes);*/
    }
}
