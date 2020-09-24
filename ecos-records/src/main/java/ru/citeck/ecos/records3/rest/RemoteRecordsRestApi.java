package ru.citeck.ecos.records3.rest;

public interface RemoteRecordsRestApi {

    <T> T jsonPost(String url, Object request, Class<T> respType);
}
