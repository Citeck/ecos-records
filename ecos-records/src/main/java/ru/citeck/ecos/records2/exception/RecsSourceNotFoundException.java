package ru.citeck.ecos.records2.exception;

import ru.citeck.ecos.records3.record.dao.RecordsDao;

public class RecsSourceNotFoundException extends RecordsException {

    private final Class<? extends RecordsDao> type;
    private final String sourceId;

    public RecsSourceNotFoundException(String sourceId, Class<? extends RecordsDao> type) {
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
