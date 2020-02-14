package ru.citeck.ecos.records2.spring.utils.web;

public interface WebUtils {

    <T> T convertRequest(byte[] body, Class<T> valueType);

    byte[] encodeResponse(Object response);
}
