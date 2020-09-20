package ru.citeck.ecos.records2.meta.schema.read;

public class AttReadException extends RuntimeException {

    public AttReadException(String alias, String attribute, String message) {
        super("Attribute read error: " + message + ". Alias: '" + alias + "' Attribute: '" + attribute + "'");
    }
}
