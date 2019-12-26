package ru.citeck.ecos.records2.resolver;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsProperties;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.RecordsDAO;
import ru.citeck.ecos.records2.utils.MandatoryParam;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.Collection;
import java.util.List;

public class LocalRemoteResolver implements RecordsResolver, RecordsDAORegistry {

    private LocalRecordsResolver local;
    private RecordsResolver remote;

    private String currentApp;
    private String currentAppSourceIdPrefix;
    private boolean forceLocalMode;

    public LocalRemoteResolver(RecordsServiceFactory serviceFactory) {

        this.remote = serviceFactory.getRemoteRecordsResolver();
        this.local = serviceFactory.getLocalRecordsResolver();

        RecordsProperties props = serviceFactory.getProperties();
        this.currentApp = props.getAppName();
        this.currentAppSourceIdPrefix = this.currentApp + "/";
        this.forceLocalMode = props.isForceLocalMode();

        MandatoryParam.check("local", local);
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {

        String sourceId = query.getSourceId();

        if (remote == null
            || StringUtils.isBlank(sourceId)
            || !sourceId.contains("/")
            || sourceId.startsWith(currentAppSourceIdPrefix)
            || forceLocalMode) {

            return local.queryRecords(query, schema);
        }
        return remote.queryRecords(query, schema);
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {
        if (remote == null || records.isEmpty()) {
            return local.getMeta(records, schema);
        }
        if (isRemoteRef(records.stream().findFirst().get())) {
            return remote.getMeta(records, schema);
        }
        return local.getMeta(records, schema);
    }

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {
        List<RecordMeta> records = mutation.getRecords();
        if (remote == null || records.isEmpty()) {
            return local.mutate(mutation);
        }
        if (isRemoteRef(records.get(0))) {
            return remote.mutate(mutation);
        }
        return local.mutate(mutation);
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        List<RecordRef> records = deletion.getRecords();
        if (remote == null || records.isEmpty()) {
            return local.delete(deletion);
        }
        if (isRemoteRef(records.get(0))) {
            return remote.delete(deletion);
        }
        return local.delete(deletion);
    }

    private boolean isRemoteRef(RecordRef ref) {
        return ref.isRemote() && !ref.getAppName().equals(currentApp) && !forceLocalMode;
    }

    private boolean isRemoteRef(RecordMeta meta) {
        return isRemoteRef(meta.getId());
    }

    @Override
    public void register(RecordsDAO recordsDao) {
        local.register(recordsDao);
    }
}
