package ru.citeck.ecos.records3.rest;

public class RestQueryException extends RuntimeException {

    private String restErrorDetails;

    public RestQueryException() {
    }

    public RestQueryException(String message) {
        super(message);
    }

    public RestQueryException(String message, String restErrorDetails) {
        super(message);
        this.restErrorDetails = restErrorDetails;
    }

    public RestQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestQueryException(String message, Throwable cause, String restErrorDetails) {
        super(message, cause);
        this.restErrorDetails = restErrorDetails;
    }

    public RestQueryException(Throwable cause) {
        super(cause);
    }

    public RestQueryException(Throwable cause, String restErrorDetails) {
        super(cause);
        this.restErrorDetails = restErrorDetails;
    }

    public RestQueryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RestQueryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
                              String restErrorDetails) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.restErrorDetails = restErrorDetails;
    }

    public String getRestErrorDetails() {
        return restErrorDetails;
    }
}
