package ru.citeck.ecos.records2.request.rest;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.List;

@Slf4j
public class RestHandler {

    private final RecordsService recordsService;
    private final RecordsServiceFactory factory;

    public RestHandler(RecordsServiceFactory factory) {
        this.factory = factory;
        this.recordsService = factory.getRecordsService();
    }

    public Object queryRecords(QueryBody body) {

        if (body.getQuery() != null && body.getRecords() != null) {
            log.warn("There must be one of 'records' or 'query' field "
                   + "but found both. 'records' field will be ignored");
        }

        RecordsResult<?> recordsResult;
        List<DataValue> foreach = body.getForeach();

        if (body.getQuery() != null) {

            if (body.getAttributes() != null) {

                if (foreach != null) {
                    recordsResult = recordsService.queryRecords(
                        foreach,
                        body.getQuery(),
                        body.getAttributes(),
                        body.isFlatAttributes()
                    );
                } else {
                    recordsResult = recordsService.queryRecords(
                        body.getQuery(),
                        body.getAttributes(),
                        body.isFlatAttributes()
                    );
                }

            } else {

                if (foreach != null) {
                    recordsResult = recordsService.queryRecords(foreach, body.getQuery());
                } else {
                    recordsResult = recordsService.queryRecords(body.getQuery());
                }
            }
        } else {

            if (body.getRecords() == null) {
                throw new IllegalArgumentException("At least 'records' or 'query' must be specified");
            }
            if (body.getAttributes() == null) {
                throw new IllegalArgumentException("You must specify 'attributes' for records");
            }
            recordsResult = recordsService.getAttributes(
                body.getRecords(),
                body.getAttributes(),
                body.isFlatAttributes()
            );
        }

        ErrorUtils.logErrorsWithBody(recordsResult, body);

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
