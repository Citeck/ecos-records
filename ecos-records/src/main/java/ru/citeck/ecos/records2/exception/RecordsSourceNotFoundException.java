package ru.citeck.ecos.records2.exception;

import ru.citeck.ecos.records2.source.dao.RecordsDao;

public class RecordsSourceNotFoundException extends RecordsException {

    private final Class<? extends RecordsDao> type;
    private final String sourceId;

    public RecordsSourceNotFoundException(String sourceId, Class<? extends RecordsDao> type) {
        super("Source is not found with id '" + sourceId + "' and "
                + "type '" + (type != null ? type.getName() : "null") + "'");

        this.sourceId = sourceId;
        this.type = type;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Class<? extends RecordsDao> getType() {
        return type;
    }
}
