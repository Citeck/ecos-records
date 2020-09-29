package ru.citeck.ecos.records3.exception;

import ru.citeck.ecos.records3.record.exception.RecordsException;

public class SourceDoesntProvideAttsException extends RecordsException {

    private String sourceId;

    public SourceDoesntProvideAttsException(String sourceId) {
        super("Source doesn't provide meta. SourceId: " + sourceId);
        this.sourceId = sourceId;
    }

    public String getSourceId() {
        return sourceId;
    }
}
