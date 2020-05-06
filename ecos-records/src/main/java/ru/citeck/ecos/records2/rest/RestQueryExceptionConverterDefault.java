package ru.citeck.ecos.records2.rest;

public class RestQueryExceptionConverterDefault implements RestQueryExceptionConverter {

    @Override
    public RestQueryException convert(String msg, Exception e) {
        if (e != null) {
            return new RestQueryException(msg, e);
        } else {
            return new RestQueryException(msg);
        }
    }

}
