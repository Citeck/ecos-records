package ru.citeck.ecos.records3.rest;

public interface RestQueryExceptionConverter {

    RestQueryException convert(String msg, Exception e);

}
