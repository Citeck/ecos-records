package ru.citeck.ecos.records3.rest;

public interface RecordsRestTemplate {

    RestResponseEntity jsonPost(String url, RestRequestEntity request);
}
