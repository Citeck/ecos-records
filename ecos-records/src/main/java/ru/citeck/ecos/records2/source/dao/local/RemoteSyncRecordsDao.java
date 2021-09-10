package ru.citeck.ecos.records2.source.dao.local;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.utils.ExceptionUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.source.dao.local.job.Job;
import ru.citeck.ecos.records2.source.dao.local.job.JobsProvider;
import ru.citeck.ecos.records2.source.dao.local.job.PeriodicJob;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.msg.ReqMsg;
import ru.citeck.ecos.records3.record.resolver.LocalRecordsResolver;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class RemoteSyncRecordsDao<T> extends InMemRecordsDao<T>
                                     implements JobsProvider {

    private static final String MODIFIED_ATT_KEY = "__sync_modified_att";
    private static final long FULL_RESET_TIME = TimeUnit.MINUTES.toMillis(5);

    private final Class<T> model;

    private final CompletableFuture<Boolean> firstSyncFuture = new CompletableFuture<>();
    private RemoteRecordsResolver remoteRecordsResolver;
    private LocalRecordsResolver localRecordsResolver;
    private DtoSchemaReader dtoSchemaReader;

    private Function<RecordsQuery, RecsQueryRes<RecordAtts>> queryImpl;

    private Instant currentSyncDate = Instant.EPOCH;
    private long lastUpdateTimeMs = 0;

    public RemoteSyncRecordsDao(String sourceId, Class<T> model) {
        super(sourceId);
        this.model = model;
    }

    @NotNull
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
        waitUntilSyncCompleted();
        return super.queryRecords(query);
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        waitUntilSyncCompleted();
        return super.getRecordsAtts(records);
    }

    @Override
    public Optional<T> getRecord(String recordRef) {
        waitUntilSyncCompleted();
        return super.getRecord(recordRef);
    }

    @Override
    public Map<String, T> getRecords() {
        waitUntilSyncCompleted();
        return super.getRecords();
    }

    private void waitUntilSyncCompleted() {
        try {
            if (!firstSyncFuture.isDone()) {
                log.warn("!!! Current thread will be blocked until data will be synchronized. "
                    + "SourceId: " + getId() + ". Timeout: 5min !!!");
                serviceFactory.initJobs(null);
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

        RecordsQuery query = RecordsQuery.create()
            .withSourceId(getId())
            .withQuery(predicate)
            .withSortBy(new SortBy(RecordConstants.ATT_MODIFIED, true))
            .withMaxItems(50)
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .build();

        RecsQueryRes<RecordAtts> result;
        try {
            result = queryImpl.apply(query);
        } catch (Exception e) {
            // target DAO is not ready yet
            return false;
        }

        List<ReqMsg> errors = RequestContext.getCurrentNotNull().getErrors();

        if (result == null || !errors.isEmpty()) {
            log.warn("Update failed: there are errors in query result. Result: " + result + " errors: " + errors);
            return false;
        }

        Instant lastModified = currentSyncDate;
        List<RecordAtts> flatMeta = result.getRecords();

        for (RecordAtts meta : flatMeta) {

            String modifiedStr = meta.getAtt(MODIFIED_ATT_KEY).asText();
            if (StringUtils.isBlank(modifiedStr)) {
                return false;
            }
            Instant modified = Instant.parse(modifiedStr);
            T instance = dtoSchemaReader.instantiate(model, meta.getAtts());

            setRecord(meta.getId().getId(), instance);

            if (modified.isAfter(lastModified)) {
                lastModified = modified;
            }
        }

        if (lastModified.isAfter(this.currentSyncDate)) {
            lastUpdateTimeMs = System.currentTimeMillis();
            this.currentSyncDate = lastModified;
        }

        if (result.getRecords().isEmpty()) {
            if (!firstSyncFuture.isDone()) {
                firstSyncFuture.complete(true);
            }
            if (System.currentTimeMillis() - lastUpdateTimeMs > FULL_RESET_TIME) {
                currentSyncDate = Instant.EPOCH;
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
        this.dtoSchemaReader = serviceFactory.getDtoSchemaReader();

        if (this.getId().contains("/") && this.remoteRecordsResolver == null) {
            throw new IllegalStateException("Sync records DAO can't work "
                + "without RemoteRecordsResolver. SourceId: " + getId());
        }

        AttSchemaWriter attSchemaWriter = serviceFactory.getAttSchemaWriter();
        List<SchemaAtt> schemaAtts = new ArrayList<>(dtoSchemaReader.read(model));

        schemaAtts.add(SchemaAtt.create()
                .withAlias(MODIFIED_ATT_KEY)
                .withName(RecordConstants.ATT_MODIFIED)
                .withInner(SchemaAtt.create().withName("?disp"))
                .build());

        if (getId().contains("/")) {
            Map<String, String> attributes = attSchemaWriter.writeToMap(schemaAtts);
            queryImpl = query -> remoteRecordsResolver.query(query, attributes, false);
        } else {
            queryImpl = query -> localRecordsResolver.query(query, schemaAtts, false);
        }
    }
}
