package ru.citeck.ecos.records3.utils.exception;

public class MethodNotImplemented extends RuntimeException {

    public MethodNotImplemented(String message) {
        super(message);
    }

    public MethodNotImplemented(String message, Throwable cause) {
        super(message, cause);
    }
}
