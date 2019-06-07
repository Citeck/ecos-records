package ru.citeck.ecos.records2.request.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.List;

public class RestHandler {

    private static final Log logger = LogFactory.getLog(RestHandler.class);

    private RecordsService recordsService;

    public RestHandler(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    public Object queryRecords(QueryBody body) {

        if (body.getQuery() != null && body.getRecords() != null) {
            logger.warn("There must be one of 'records' or 'query' field "
                        + "but found both. 'records' field will be ignored");
        }
        if (body.getAttributes() != null && body.getSchema() != null) {
            logger.warn("There must be one of 'attributes' or 'schema' field "
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

        if (body.isSingleRecord()) {

            Object record = recordsResult.getRecords()
                                         .stream()
                                         .findFirst()
                                         .orElseThrow(() -> new IllegalStateException("Records is empty"));

            if (body.isSingleAttribute() && record instanceof RecordMeta) {

                RecordMeta meta = (RecordMeta) record;
                return meta.get(QueryBody.SINGLE_ATT_KEY);

            } else {

                return record;
            }
        }

        return recordsResult;
    }

    public Object mutateRecords(MutationBody body) {

        RecordsMutResult result = recordsService.mutate(body);

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
}
