package ru.citeck.ecos.records2.resolver;

import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.RecordsDAO;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.util.Collection;
import java.util.List;

public class LocalRemoteResolver implements RecordsResolver, RecordsDAORegistry {

    private final LocalRecordsResolver local;
    private final RecordsResolver remote;

    private final String currentAppSourceIdPrefix;

    public LocalRemoteResolver(RecordsServiceFactory serviceFactory) {

        this.remote = serviceFactory.getRemoteRecordsResolver();
        this.local = serviceFactory.getLocalRecordsResolver();

        RecordsProperties props = serviceFactory.getProperties();
        this.currentAppSourceIdPrefix = props.getAppName() + "/";

        MandatoryParam.check("local", local);
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {

        String sourceId = query.getSourceId();

        if (remote == null || !isRemoteSourceId(sourceId)) {
            return local.queryRecords(query, schema);
        }
        return remote.queryRecords(query, schema);
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {
        if (remote == null || records.isEmpty()) {
            return local.getMeta(records, schema);
        }
        RecordsResult<RecordMeta> result = new RecordsResult<>();
        RecordsUtils.groupRefBySource(records).forEach((sourceId, recs) -> {

            if (!isRemoteSourceId(sourceId)) {
                result.merge(local.getMeta(records, schema));
            } else if (isRemoteRef(records.stream().findFirst().orElse(null))) {
                result.merge(remote.getMeta(records, schema));
            } else {
                result.merge(local.getMeta(records, schema));
            }
        });
        return result;
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

    private boolean isRemoteRef(RecordMeta meta) {
        return isRemoteRef(meta.getId());
    }

    private boolean isRemoteRef(RecordRef ref) {
        return ref != null && ref.isRemote() && isRemoteSourceId(ref.getAppName() + "/" + ref.getSourceId());
    }

    private boolean isRemoteSourceId(String sourceId) {
        if (StringUtils.isBlank(sourceId)) {
            return false;
        }
        if (local.containsDao(sourceId)) {
            return false;
        }
        return sourceId.contains("/") && !sourceId.startsWith(currentAppSourceIdPrefix);
    }

    @Override
    public void register(RecordsDAO recordsDao) {
        local.register(recordsDao);
    }
}
