package ru.citeck.ecos.records3.source.dao.remote;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsMetaDao;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.rest.QueryBody;
import ru.citeck.ecos.records3.source.dao.*;

import java.util.List;

@Slf4j
public class RemoteRecordsDao extends AbstractRecordsDao
                              implements RecordsMetaDao,
                                         RecordsQueryDao {

    private boolean enabled = true;

    private RecordsRestConnection restConnection;

    private String recordsMethod = "/api/ecos/records";
    private String remoteSourceId = null;

    @NotNull
    @Override
    public RecsQueryRes<Object> queryRecords(@NotNull RecordsQuery query) {

        /*QueryBody request = new QueryBody();

        if (enabled) {

            prepareQueryBody(request, query);
            request.setAttributes(schema.getAttributes());

            RecordsMetaQueryResult result = restConnection.jsonPost(recordsMethod,
                                                                    request,
                                                                    RecordsMetaQueryResult.class);
            if (result != null) {
                return result.addSourceId(getId());
            } else {
                log.error("[" + getId() + "] queryRecords will return nothing. " + request);
            }
        }
        return new RecordsMetaQueryResult();*/
        return null;
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

    @NotNull
    @Override
    public List<Object> getRecordsMeta(@NotNull List<RecordRef> records) {

       /* List<RecordRef> recordsRefs = records.stream()
                                             .map(RecordRef::getId)
                                             .map(RecordRef::valueOf)
                                             .collect(Collectors.toList());

        QueryBody request = new QueryBody();
        request.setAttributes(schema.getAttributes());
        request.setRecords(recordsRefs);

        RecordsMetaResult nodesResult = restConnection.jsonPost(recordsMethod, request, RecordsMetaResult.class);

        List<RecordMeta> restResultRecords = nodesResult.getRecords();
        List<RecordMeta> meta = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            meta.add(new RecordMeta(restResultRecords.get(i), records.get(i)));
        }

        nodesResult.setRecords(meta);

        return nodesResult;*/
        return null;
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

    @Override
    public boolean isRawAttributesProvided() {
        return false;
    }
}
