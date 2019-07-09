package ru.citeck.ecos.records2.exception;

public class SourceDoesntProvideMetaException extends RecordsException {

    private String sourceId;

    public SourceDoesntProvideMetaException(String sourceId) {
        super("Source doesn't provide meta. SourceId: " + sourceId);
        this.sourceId = sourceId;
    }

    public String getSourceId() {
        return sourceId;
    }
}
