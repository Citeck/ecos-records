package ru.citeck.ecos.records3.record.op.meta.schema.read;

public class AttReadException extends RuntimeException {

    public AttReadException(String alias, String attribute, String message) {
        super("Attribute read error: " + message + ". Alias: '" + alias + "' Attribute: '" + attribute + "'");
    }
}
