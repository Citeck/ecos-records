package ru.citeck.ecos.records3.rest;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.delete.DeletionBody;
import ru.citeck.ecos.records3.record.operation.mutate.MutateBody;

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

        //RecordsResult<?> recordsResult;
        /*List<DataValue> foreach = body.getForeach();

        if (body.getQuery() != null) {

            if (body.getAttributes() != null) {

                if (foreach != null) {
                    recordsResult = recordsService.query(
                        foreach,
                        body.getQuery(),
                        body.getAttributes(),
                        body.isFlatAttributes()
                    );
                } else {
                    recordsResult = recordsService.query(
                        body.getQuery(),
                        body.getAttributes(),
                        body.isFlatAttributes()
                    );
                }

            } else {

                if (foreach != null) {
                    recordsResult = recordsService.query(foreach, body.getQuery());
                } else {
                    recordsResult = recordsService.query(body.getQuery());
                }
            }
        } else {

            if (body.getRecords() == null) {
                throw new IllegalArgumentException("At least 'records' or 'query' must be specified");
            }
            if (body.getAttributes() == null) {
                throw new IllegalArgumentException("You must specify 'attributes' for records");
            }
            recordsResult = recordsService.getAtts(
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

            if (body.isSingleAttribute() && record instanceof RecordAtts) {

                RecordAtts meta = (RecordAtts) record;
                return meta.get(QueryBody.SINGLE_ATT_KEY);

            } else {

                return record;
            }
        }

        return recordsResult;*/
        return null;
    }

    public Object mutateRecords(MutateBody body) {

        /*RecordsMutResult result = recordsService.mutate(body);

        ErrorUtils.logErrorsWithBody(result, body);

        if (body.isSingleRecord()) {
            List<?> records = result.getRecords();
            if (records.isEmpty()) {
                throw new IllegalStateException("Records list is empty");
            }
            return records.get(0);
        }

        return result;*/
        return null;
    }


    public Object deleteRecords(DeletionBody body) {
        //return recordsService.delete(body);
        return null;
    }

    public RecordsServiceFactory getFactory() {
        return factory;
    }
}
