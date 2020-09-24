package ru.citeck.ecos.records3.record.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.record.operation.delete.request.RecordsDelResult;
import ru.citeck.ecos.records3.record.operation.delete.request.RecordsDeletion;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutResult;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutation;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.request.result.RecordsResult;
import ru.citeck.ecos.records3.source.dao.RecordsDao;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;
import ru.citeck.ecos.records3.utils.RecordsUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class LocalRemoteResolver implements RecordsResolver, RecordsDaoRegistry {

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

    @Nullable
    @Override
    public RecsQueryRes<RecordMeta> queryRecords(@NotNull RecordsQuery query,
                                                 @NotNull Map<String, String> attributes,
                                                 boolean flat) {

        String sourceId = query.getSourceId();

        if (remote == null || !isRemoteSourceId(sourceId)) {
            return local.queryRecords(query, attributes, flat);
        }
        return remote.queryRecords(query, attributes, flat);
    }

    @NotNull
    @Override
    public RecordsResult<RecordMeta> getMeta(@NotNull Collection<RecordRef> records,
                                             @NotNull Map<String, String> attributes,
                                             boolean flat) {

        if (remote == null || records.isEmpty()) {
            return local.getMeta(records, attributes, flat);
        }
        RecordsResult<RecordMeta> result = new RecordsResult<>();
        RecordsUtils.groupRefBySource(records).forEach((sourceId, recs) -> {

            if (!isRemoteSourceId(sourceId)) {
                result.merge(local.getMeta(records, attributes, flat));
            } else if (isRemoteRef(records.stream().findFirst().orElse(null))) {
                result.merge(remote.getMeta(records, attributes, flat));
            } else {
                result.merge(local.getMeta(records, attributes, flat));
            }
        });
        return result;
    }

    @Nullable
    @Override
    public RecordsMutResult mutate(@NotNull RecordsMutation mutation) {
        List<RecordMeta> records = mutation.getRecords();
        if (remote == null || records.isEmpty()) {
            return local.mutate(mutation);
        }
        if (isRemoteRef(records.get(0))) {
            return remote.mutate(mutation);
        }
        return local.mutate(mutation);
    }

    @NotNull
    @Override
    public RecordsDelResult delete(@NotNull RecordsDeletion deletion) {
        List<RecordRef> records = deletion.getRecords();
        if (remote == null || records.isEmpty()) {
            return local.delete(deletion);
        }
        if (isRemoteRef(records.get(0))) {
            return remote.delete(deletion);
        }
        return local.delete(deletion);
    }

    @Nullable
    @Override
    public RecsSourceInfo getSourceInfo(@NotNull String sourceId) {
        if (isRemoteSourceId(sourceId)) {
            return remote.getSourceInfo(sourceId);
        }
        return local.getSourceInfo(sourceId);
    }

    @NotNull
    @Override
    public List<RecsSourceInfo> getSourceInfo() {
        List<RecsSourceInfo> result = new ArrayList<>(local.getSourceInfo());
        result.addAll(remote.getSourceInfo());
        return result;
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
    public void register(String sourceId, RecordsDao recordsDao) {
        local.register(sourceId, recordsDao);
    }
}
