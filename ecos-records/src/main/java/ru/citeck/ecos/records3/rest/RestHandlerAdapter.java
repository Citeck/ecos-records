package ru.citeck.ecos.records3.rest;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.JsonMapper;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.rest.DeletionBody;
import ru.citeck.ecos.records2.request.rest.MutationBody;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody;
import ru.citeck.ecos.records3.rest.v1.query.QueryBody;
import ru.citeck.ecos.records3.rest.v1.RestHandlerV1;

@Slf4j
public class RestHandlerAdapter {

    private final RestHandler restHandlerV0;
    private final RestHandlerV1 restHandlerV1;
    private final JsonMapper mapper = Json.getMapper();

    public RestHandlerAdapter(RecordsServiceFactory serviceFactory) {
        restHandlerV1 = new RestHandlerV1(serviceFactory);
        restHandlerV0 = serviceFactory.getRestHandler();
    }

    public Object queryRecords(Object body) {

        BodyWithVersion bodyWithVersion = getBodyWithVersion(body);

        switch (bodyWithVersion.version) {
            case 0:

                ru.citeck.ecos.records2.request.rest.QueryBody v0Body =
                    mapper.convert(bodyWithVersion.getBody(),
                        ru.citeck.ecos.records2.request.rest.QueryBody.class);

                if (v0Body == null) {
                    v0Body = new ru.citeck.ecos.records2.request.rest.QueryBody();
                }
                return restHandlerV0.queryRecords(v0Body);

            case 1:

                return restHandlerV1.queryRecords(mapper.convert(bodyWithVersion, QueryBody.class));

            default:
                throw new IllegalArgumentException("Unknown body version. Body: " + bodyWithVersion);
        }
    }

    public Object deleteRecords(Object body) {

        BodyWithVersion bodyWithVersion = getBodyWithVersion(body);

        switch (bodyWithVersion.version) {
            case 0:

                DeletionBody v0Body = mapper.convert(bodyWithVersion.getBody(), DeletionBody.class);

                if (v0Body == null) {
                    v0Body = new DeletionBody();
                }
                return restHandlerV0.deleteRecords(v0Body);

            case 1:

                return restHandlerV1.deleteRecords(mapper.convert(bodyWithVersion, DeleteBody.class));

            default:
                throw new IllegalArgumentException("Unknown body version. Body: " + bodyWithVersion);
        }
    }

    public Object mutateRecords(Object body) {

        BodyWithVersion bodyWithVersion = getBodyWithVersion(body);

        switch (bodyWithVersion.version) {
            case 0:

                MutationBody v0Body = mapper.convert(bodyWithVersion.getBody(), MutationBody.class);

                if (v0Body == null) {
                    v0Body = new MutationBody();
                }
                return restHandlerV0.mutateRecords(v0Body);

            case 1:

                return restHandlerV1.mutateRecords(mapper.convert(bodyWithVersion, MutateBody.class));

            default:
                throw new IllegalArgumentException("Unknown body version. Body: " + bodyWithVersion);
        }
    }

    private BodyWithVersion getBodyWithVersion(Object body) {

        ObjectNode jsonBody = Json.getMapper().convert(body, ObjectNode.class);
        if (jsonBody == null) {
            throw new IllegalArgumentException("Incorrect request body. Expected JSON Object, but found: " + body);
        }

        JsonNode version = jsonBody.path("version");
        if (version.isNumber()) {
            return new BodyWithVersion(jsonBody, version.asInt());
        }

        JsonNode v1Body = jsonBody.path("v1Body");
        if (v1Body instanceof ObjectNode && v1Body.size() > 0) {
            return new BodyWithVersion((ObjectNode) v1Body, 1);
        }

        return new BodyWithVersion(jsonBody, 0);
    }

    @Data
    @AllArgsConstructor
    private static class BodyWithVersion {
        private final ObjectNode body;
        private final int version;
    }
}
