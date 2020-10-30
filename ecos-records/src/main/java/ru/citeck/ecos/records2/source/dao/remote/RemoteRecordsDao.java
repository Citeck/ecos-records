package ru.citeck.ecos.records2.source.dao.remote;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.NullNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records2.meta.AttributesSchema;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.typed.RecordsMetaQueryResult;
import ru.citeck.ecos.records2.request.query.typed.RecordsMetaResult;
import ru.citeck.ecos.records2.request.query.typed.RecordsRefsQueryResult;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.AbstractRecordsDao;
import ru.citeck.ecos.records2.source.dao.RecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDao;
import ru.citeck.ecos.records2.source.dao.RecordsQueryWithMetaDao;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RemoteRecordsDao extends AbstractRecordsDao
                              implements RecordsMetaDao,
                                         RecordsQueryWithMetaDao,
                                         RecordsQueryDao,
                                         ServiceFactoryAware {

    private boolean enabled = true;

    private RecordsRestConnection restConnection;
    private RecordsServiceFactory services;
    private RecordsMetaService recordsMetaService;

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
    public RecordsQueryResult<RecordAtts> queryRecords(RecordsQuery query, List<SchemaAtt> schema, boolean rawAtts) {

        QueryBody request = new QueryBody();

        if (enabled) {

            prepareQueryBody(request, query);

            Map<String, String> attsMap = services.getAttSchemaWriter().writeToMap(schema);
            AttributesSchema attsSchema = services.getAttributesMetaResolver().createSchema(attsMap);

            request.setSchema(attsSchema.getSchema());

            RecordsMetaQueryResult result = restConnection.jsonPost(recordsMethod,
                                                                    request,
                                                                    RecordsMetaQueryResult.class);

            result.setRecords(recordsMetaService.convertMetaResult(result.getRecords(), attsSchema, !rawAtts));

            return result.addSourceId(getId());
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
    public RecordsResult<RecordAtts> getMeta(List<RecordRef> records, List<SchemaAtt> schema, boolean rawAtts) {

        List<RecordRef> recordsRefs = records.stream()
                                             .map(RecordRef::getId)
                                             .map(RecordRef::valueOf)
                                             .collect(Collectors.toList());

        Map<String, String> attsMap = services.getAttSchemaWriter().writeToMap(schema);
        AttributesSchema attsSchema = services.getAttributesMetaResolver().createSchema(attsMap);

        QueryBody request = new QueryBody();
        request.setSchema(attsSchema.getSchema());
        request.setRecords(recordsRefs);

        RecordsMetaResult nodesResult = restConnection.jsonPost(recordsMethod, request, RecordsMetaResult.class);

        List<RecordAtts> restResultRecords = nodesResult.getRecords();
        List<RecordAtts> meta = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            RecordAtts resAtts = restResultRecords.get(i);
            resAtts = recordsMetaService.convertMetaResult(resAtts, attsSchema, !rawAtts);
            meta.add(new RecordAtts(resAtts, records.get(i)));
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

    public void setRecordsServiceFactory(RecordsServiceFactory serviceFactory) {
        this.services = serviceFactory;
        this.recordsMetaService = services.getRecordsMetaService();
    }
}
