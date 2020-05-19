package ru.citeck.ecos.records2.rest;

public interface RemoteRecordsRestApi {

    <T> T jsonPost(String url, Object request, Class<T> respType);
}
