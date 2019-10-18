package ru.citeck.ecos.records2.exception;

import ru.citeck.ecos.records2.source.dao.RecordsDAO;

public class RecordsSourceNotFoundException extends RecordsException {

    private Class<? extends RecordsDAO> type;
    private String sourceId;

    public RecordsSourceNotFoundException(String sourceId, Class<? extends RecordsDAO> type) {
        super("Source is not found with id '" + sourceId + "' and "
                + "type '" + (type != null ? type.getName() : "null") + "'");

        this.sourceId = sourceId;
        this.type = type;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Class<? extends RecordsDAO> getType() {
        return type;
    }
}
