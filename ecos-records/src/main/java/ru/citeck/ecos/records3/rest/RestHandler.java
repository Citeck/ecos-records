package ru.citeck.ecos.records3.rest;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.rest.v1.QueryBody;
import ru.citeck.ecos.records3.rest.v1.RestHandlerV1;

@Slf4j
public class RestHandler {

    private final RestHandlerV1 restHandlerV1;

    public RestHandler(RecordsServiceFactory serviceFactory) {
        restHandlerV1 = new RestHandlerV1(serviceFactory);
    }

    public Object queryRecords(Object body) {

        BodyWithVersion bodyWithVersion = getBodyWithVersion(body);
        switch (bodyWithVersion.version) {
            case 0:
                //todo

            case 1:
                return restHandlerV1.queryRecords(Json.getMapper().convert(bodyWithVersion, QueryBody.class));
            default:
                throw new IllegalArgumentException("Unknown body version. Body: " + bodyWithVersion);
        }
    }

    public Object deleteRecords(Object body) {

    }

    public Object mutateRecords(Object body) {

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
        return new BodyWithVersion(jsonBody, 0);
    }

    @Data
    @AllArgsConstructor
    private static class BodyWithVersion {
        ObjectNode body;
        int version;
    }
}
