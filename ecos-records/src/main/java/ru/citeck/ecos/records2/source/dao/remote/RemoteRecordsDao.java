package ru.citeck.ecos.records2.source.dao.remote;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.typed.RecordsMetaQueryResult;
import ru.citeck.ecos.records2.request.query.typed.RecordsMetaResult;
import ru.citeck.ecos.records2.request.query.typed.RecordsRefsQueryResult;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RemoteRecordsDao extends AbstractRecordsDao
                              implements RecordsMetaDao,
                                         RecordsQueryWithMetaDao,
                                         RecordsQueryDao {

    private boolean enabled = true;

    private RecordsRestConnection restConnection;

    private String recordsMethod = "/api/ecos/records";
    private String remoteSourceId = null;

    @Override
    public RecordsRefsQueryResult queryRecords(RecordsQuery query) {

        QueryBody request = new QueryBody();

        if (enabled) {

            prepareQueryBody(request, query);

            RecordsRefsQueryResult result = restConnection.jsonPost(recordsMethod,
                                                                    request,
                                                                    RecordsRefsQueryResult.class);
            if (result != null) {
                return result.addSourceId(getId());
            } else {
                log.error("[" + getId() + "] queryRecords will return nothing. " + request);
            }
        }
        return new RecordsRefsQueryResult();
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String metaSchema) {

        QueryBody request = new QueryBody();

        if (enabled) {

            prepareQueryBody(request, query);
            request.setSchema(metaSchema);

            RecordsMetaQueryResult result = restConnection.jsonPost(recordsMethod,
                                                                    request,
                                                                    RecordsMetaQueryResult.class);
            if (result != null) {
                return result.addSourceId(getId());
            } else {
                log.error("[" + getId() + "] queryRecords will return nothing. " + request);
            }
        }
        return new RecordsMetaQueryResult();
    }

    private void prepareQueryBody(QueryBody body, RecordsQuery query) {
        RecordRef afterId = query.getAfterId();
        RecordsQuery bodyQuery = new RecordsQuery(query);
        body.setQuery(bodyQuery);
        if (remoteSourceId != null) {
            bodyQuery.setSourceId(remoteSourceId);
        }
        if (afterId != RecordRef.EMPTY) {
            bodyQuery.setAfterId(RecordRef.valueOf(afterId.getId()));
        }
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(List<RecordRef> records, String gqlSchema) {

        List<RecordRef> recordsRefs = records.stream()
                                             .map(RecordRef::getId)
                                             .map(RecordRef::valueOf)
                                             .collect(Collectors.toList());

        QueryBody request = new QueryBody();
        request.setSchema(gqlSchema);
        request.setRecords(recordsRefs);

        RecordsMetaResult nodesResult = restConnection.jsonPost(recordsMethod, request, RecordsMetaResult.class);

        List<RecordMeta> restResultRecords = nodesResult.getRecords();
        List<RecordMeta> meta = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            meta.add(new RecordMeta(restResultRecords.get(i), records.get(i)));
        }

        nodesResult.setRecords(meta);

        return nodesResult;
    }

    public void setRecordsMethod(String recordsMethod) {
        this.recordsMethod = recordsMethod;
    }

    public void setRemoteSourceId(String remoteSourceId) {
        this.remoteSourceId = remoteSourceId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRestConnection(RecordsRestConnection restConnection) {
        this.restConnection = restConnection;
    }

    public RecordsRestConnection getRestConnection() {
        return restConnection;
    }
}
