package ru.citeck.ecos.records2.exception;

public class RecsSourceNotFoundException extends RecordsException {

    private final Class<?> type;
    private final String sourceId;

    public RecsSourceNotFoundException(String sourceId, Class<?> type) {
        super("Source is not found with id '" + sourceId + "' and "
                + "type '" + (type != null ? type.getName() : "null") + "'");

        this.sourceId = sourceId;
        this.type = type;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Class<?> getType() {
        return type;
    }
}
