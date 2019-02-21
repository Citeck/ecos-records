package ru.citeck.ecos.records2.source.dao.remote;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.request.query.typed.RecordsMetaResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.typed.RecordsRefsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.AbstractRecordsDAO;
import ru.citeck.ecos.records2.source.dao.RecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDAO;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.util.List;
import java.util.stream.Collectors;

public class RemoteRecordsDAO extends AbstractRecordsDAO
                              implements RecordsMetaDAO,
                                         RecordsQueryDAO {

    private static final Log logger = LogFactory.getLog(RemoteRecordsDAO.class);

    private boolean enabled = true;

    private RecordsRestConnection restConnection;

    private String recordsMethod = "/api/ecos/records";
    private String remoteSourceId = "";

    @Override
    public RecordsRefsQueryResult getRecords(RecordsQuery query) {

        QueryBody request = new QueryBody();

        if (enabled) {

            RecordRef afterId = query.getAfterId();
            request.setQuery(new RecordsQuery(query));
            request.getQuery().setSourceId(remoteSourceId);
            if (afterId != RecordRef.EMPTY) {
                request.getQuery().setAfterId(new RecordRef(afterId.getId()));
            }

            RecordsRefsQueryResult result = restConnection.jsonPost(recordsMethod,
                                                                    request,
                                                                    RecordsRefsQueryResult.class);
            if (result != null) {
                return result.addSourceId(getId());
            } else {
                logger.error("[" + getId() + "] queryRecords will return nothing. " + request);
            }
        }
        return new RecordsRefsQueryResult();
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(List<RecordRef> records, String gqlSchema) {

        List<RecordRef> recordsRefs = records.stream()
                                             .map(RecordRef::getId)
                                             .map(RecordRef::new)
                                             .collect(Collectors.toList());

        QueryBody request = new QueryBody();
        request.setSchema(gqlSchema);
        request.setRecords(recordsRefs);

        RecordsMetaResult nodesResult = restConnection.jsonPost(recordsMethod, request, RecordsMetaResult.class);
        List<RecordMeta> meta = RecordsUtils.convertToRefs(getId(), nodesResult.getRecords());
        nodesResult.setRecords(meta);

        return nodesResult;
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
