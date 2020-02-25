package ru.citeck.ecos.records2.rest;

public class RestQueryException extends RuntimeException {

    public RestQueryException() {
    }

    public RestQueryException(String message) {
        super(message);
    }

    public RestQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestQueryException(Throwable cause) {
        super(cause);
    }

    public RestQueryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
