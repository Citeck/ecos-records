package ru.citeck.ecos.records2.exception;

public class RecordsException extends RuntimeException {

    public RecordsException(String message) {
        super(message);
    }

    public RecordsException(String message, Throwable cause) {
        super(message, cause);
    }
}
