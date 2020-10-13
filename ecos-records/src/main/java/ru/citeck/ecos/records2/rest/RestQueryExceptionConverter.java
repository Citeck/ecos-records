package ru.citeck.ecos.records2.rest;

public interface RestQueryExceptionConverter {

    RestQueryException convert(String msg, Exception e);

}
