package ru.citeck.ecos.records2.source.dao.remote;

public interface RecordsRestConnection {

    <T> T jsonPost(String url, Object request, Class<T> resultType);

}
