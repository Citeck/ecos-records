package ru.citeck.ecos.records3.record.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.record.operation.delete.DelStatus;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.RecordsDao;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;
import ru.citeck.ecos.records3.utils.RecordsUtils;

import java.util.ArrayList;
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
    public RecordsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                             @NotNull Map<String, String> attributes,
                                             boolean rawAtts) {

        String sourceId = query.getSourceId();

        if (remote == null || !isRemoteSourceId(sourceId)) {
            return local.query(query, attributes, rawAtts);
        }
        return remote.query(query, attributes, rawAtts);
    }

    @NotNull
    @Override
    public List<RecordAtts> getAtts(@NotNull List<RecordRef> records,
                                             @NotNull Map<String, String> attributes,
                                             boolean rawAtts) {

        if (remote == null || records.isEmpty()) {
            return local.getAtts(records, attributes, rawAtts);
        }
        List<RecordAtts> result = new ArrayList<>();
        RecordsUtils.groupRefBySource(records).forEach((sourceId, recs) -> {

            /*if (!isRemoteSourceId(sourceId)) {
                result.merge(local.getMeta(records, attributes, rawAtts));
            } else if (isRemoteRef(records.stream().findFirst().orElse(null))) {
                result.merge(remote.getMeta(records, attributes, rawAtts));
            } else {
                result.merge(local.getMeta(records, attributes, rawAtts));
            }*/
        });
        return result;
    }

    @Nullable
    @Override
    public List<RecordRef> mutate(@NotNull List<RecordAtts> records) {
        if (remote == null || records.isEmpty()) {
            return local.mutate(records);
        }
        if (isRemoteRef(records.get(0))) {
            return remote.mutate(records);
        }
        return local.mutate(records);
    }

    @NotNull
    @Override
    public List<DelStatus> delete(@NotNull List<RecordRef> records) {
        if (remote == null || records.isEmpty()) {
            return local.delete(records);
        }
        if (isRemoteRef(records.get(0))) {
            return remote.delete(records);
        }
        return local.delete(records);
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

    private boolean isRemoteRef(RecordAtts meta) {
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
