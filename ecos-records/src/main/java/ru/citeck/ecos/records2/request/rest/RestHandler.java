package ru.citeck.ecos.records2.request.rest;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.List;

@Slf4j
public class RestHandler {

    private RecordsService recordsService;
    private RecordsServiceFactory factory;

    @Deprecated
    public RestHandler(RecordsService recordsService) {
        log.error("Constructor with recordsService is deprecated");
        this.recordsService = recordsService;
    }

    public RestHandler(RecordsServiceFactory factory) {
        this.factory = factory;
        this.recordsService = factory.getRecordsService();
    }

    public Object queryRecords(QueryBody body) {

        if (body.getQuery() != null && body.getRecords() != null) {
            log.warn("There must be one of 'records' or 'query' field "
                   + "but found both. 'records' field will be ignored");
        }
        if (body.getAttributes() != null && body.getSchema() != null) {
            log.warn("There must be one of 'attributes' or 'schema' field "
                   + "but found both. 'schema' field will be ignored");
        }

        RecordsResult<?> recordsResult;

        if (body.getQuery() != null) {

            if (body.getAttributes() != null) {

                recordsResult = recordsService.queryRecords(body.getQuery(), body.getAttributes());

            } else if (body.getSchema() != null) {

                recordsResult = recordsService.queryRecords(body.getQuery(), body.getSchema());

            } else {

                recordsResult = recordsService.queryRecords(body.getQuery());
            }
        } else {

            if (body.getRecords() == null) {
                throw new IllegalArgumentException("At least 'records' or 'query' must be specified");
            }
            if (body.getSchema() == null && body.getAttributes() == null) {
                throw new IllegalArgumentException("You must specify 'schema' or 'attributes' for records");
            }

            if (body.getAttributes() == null) {

                recordsResult = recordsService.getMeta(body.getRecords(), body.getSchema());

            } else {

                recordsResult = recordsService.getAttributes(body.getRecords(), body.getAttributes());
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

        RecordsMutResult result = recordsService.mutate(body);

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
        return recordsService.delete(body);
    }

    public RecordsServiceFactory getFactory() {
        return factory;
    }
}
