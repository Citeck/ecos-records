package ru.citeck.ecos.records2.request.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RestQueryHandler {

    private static final Log logger = LogFactory.getLog(RestQueryHandler.class);

    private RecordsService recordsService;

    public RestQueryHandler(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    public Object queryRecords(QueryBody body) {

        if (body.getQuery() != null && body.getRecords() != null) {
            logger.warn("There must be one of 'records' or 'query' field " +
                        "but found both. 'records' field will be ignored");
        }
        if (body.getAttributes() != null && body.getSchema() != null) {
            logger.warn("There must be one of 'attributes' or 'schema' field " +
                        "but found both. 'schema' field will be ignored");
        }

        RecordsResult<?> recordsResult;

        if (body.getQuery() != null) {

            if (body.getAttributes() != null) {

                recordsResult = recordsService.getRecords(body.getQuery(), getAttributes(body));

            } else if (body.getSchema() != null) {

                recordsResult = recordsService.getRecords(body.getQuery(), body.getSchema());

            } else {

                recordsResult = recordsService.getRecords(body.getQuery());
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

                recordsResult = recordsService.getAttributes(body.getRecords(), getAttributes(body));
            }
        }

        if (body.isSingleRecord()) {
            return recordsResult.getRecords()
                                .stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Records is empty"));
        }

        return recordsResult;
    }

    private Map<String, String> getAttributes(QueryBody body) {

        Map<String, String> result = new HashMap<>();

        if (body.getAttributes().isArray()) {
            for (int i = 0; i < body.getAttributes().size(); i++) {
                String field = body.getAttributes().get(i).asText();
                result.put(field, field);
            }
        } else {
            Iterator<String> names = body.getAttributes().fieldNames();
            while (names.hasNext()) {
                String fieldKey = names.next();
                result.put(fieldKey, body.getAttributes().get(fieldKey).asText());
            }
        }

        return result;
    }
}
