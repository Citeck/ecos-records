package ru.citeck.ecos.records2.request.rest;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.txn.RecordsTxnService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class RestHandler {

    private final RecordsService recordsService;
    private final RecordsServiceFactory factory;
    private final RecordsTxnService recordsTxnService;

    private final AttsLegacySchemaParser legacySchemaParser;

    public RestHandler(RecordsServiceFactory factory) {
        this.factory = factory;
        this.recordsService = factory.getRecordsService();
        this.recordsTxnService = factory.getRecordsTxnService();
        this.legacySchemaParser = new AttsLegacySchemaParser(factory);
    }

    public Object queryRecords(QueryBody body) {
        return recordsTxnService.doInTransaction(true, () -> queryRecordsImpl(body));
    }

    private Object queryRecordsImpl(QueryBody body) {

        if (body.getQuery() != null && body.getRecords() != null) {
            log.warn("There must be one of 'records' or 'query' field "
                   + "but found both. 'records' field will be ignored");
        }
        if (body.getAttributes() != null && body.getSchema() != null) {
            log.warn("There must be one of 'attributes' or 'schema' field "
                   + "but found both. 'schema' field will be ignored");
        }

        Map<String, String> attributes = body.getAttributes();
        if (attributes == null || attributes.isEmpty() && body.getSchema() != null) {
            attributes = legacySchemaParser.parse(body.getSchema());
        }

        RecordsResult<?> recordsResult;

        if (body.getQuery() != null) {

            if (attributes != null) {

                recordsResult = recordsService.queryRecords(body.getQuery(), attributes);

            } else {

                recordsResult = recordsService.queryRecords(body.getQuery());
            }
        } else {

            if (body.getRecords() == null) {
                throw new IllegalArgumentException("At least 'records' or 'query' must be specified");
            }
            if (body.getSchema() == null && attributes == null) {
                throw new IllegalArgumentException("You must specify 'schema' or 'attributes' for records");
            }

            if (attributes == null) {

                recordsResult = recordsService.getAttributes(body.getRecords(), Collections.emptyList());

            } else {

                recordsResult = recordsService.getAttributes(body.getRecords(), attributes);
            }
        }

        ErrorUtils.logErrorsWithBody(recordsResult, body);

        if (body.isSingleRecord()) {

            Object record = recordsResult.getRecords()
                                         .stream()
                                         .findFirst()
                                         .orElseThrow(() -> new IllegalStateException("Records is empty"));

            if (body.isSingleAttribute() && record instanceof RecordMeta) {

                RecordMeta meta = (RecordMeta) record;
                return meta.getAtt(QueryBody.SINGLE_ATT_KEY);

            } else {

                return record;
            }
        }

        return recordsResult;
    }

    public Object mutateRecords(MutationBody body) {

        RecordsMutResult result = recordsTxnService.doInTransaction(false, () -> recordsService.mutate(body));

        ErrorUtils.logErrorsWithBody(result, body);

        if (body.isSingleRecord()) {
            List<?> records = result.getRecords();
            if (records.isEmpty()) {
                throw new IllegalStateException("Records list is empty");
            }
            return records.get(0);
        }

        return result;
    }

    public Object deleteRecords(DeletionBody body) {
        return recordsTxnService.doInTransaction(false, () -> recordsService.delete(body));
    }

    public RecordsServiceFactory getFactory() {
        return factory;
    }
}
