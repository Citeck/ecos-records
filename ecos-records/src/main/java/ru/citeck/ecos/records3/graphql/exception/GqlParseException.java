package ru.citeck.ecos.records3.graphql.exception;

public class GqlParseException extends RuntimeException {

    public GqlParseException(String message, String schema, Exception cause) {
        super(message + "\nschema: '" + schema + "'", cause);
    }
}
