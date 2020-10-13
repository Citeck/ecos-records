package ru.citeck.ecos.records2.rest;

public interface RecordsRestTemplate {

    RestResponseEntity jsonPost(String url, RestRequestEntity request);
}
