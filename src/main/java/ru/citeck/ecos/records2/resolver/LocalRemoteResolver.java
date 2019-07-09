package ru.citeck.ecos.records2.resolver;

import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.meta.RecordsMetaServiceAware;
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
import java.util.Optional;

public class LocalRemoteResolver implements RecordsResolver,
                                            RecordsDAORegistry,
                                            RecordsServiceAware,
                                            RecordsMetaServiceAware,
                                            PredicateServiceAware {

    private LocalRecordsResolver local;
    private RecordsResolver remote;

    public LocalRemoteResolver(LocalRecordsResolver local, RecordsResolver remote) {
        this.remote = remote;
        this.local = local;
        MandatoryParam.check("local", local);
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {
        String sourceId = query.getSourceId();
        if (remote == null || StringUtils.isBlank(sourceId) || !sourceId.contains("/")) {
            return local.queryRecords(query, schema);
        }
        return remote.queryRecords(query, schema);
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {
        if (remote == null || records.isEmpty()) {
            return local.getMeta(records, schema);
        }
        Optional<RecordRef> first = records.stream().findFirst();
        if (first.get().isRemote()) {
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
        if (records.get(0).getId().isRemote()) {
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
        if (records.get(0).isRemote()) {
            return remote.delete(deletion);
        }
        return local.delete(deletion);
    }

    @Override
    public void register(RecordsDAO recordsDao) {
        local.register(recordsDao);
    }

    @Override
    public void setPredicateService(PredicateService predicateService) {
        local.setPredicateService(predicateService);
    }

    @Override
    public void setRecordsService(RecordsService recordsService) {
        local.setRecordsService(recordsService);
    }

    @Override
    public void setRecordsMetaService(RecordsMetaService recordsMetaService) {
        local.setRecordsMetaService(recordsMetaService);
    }
}
