package ru.citeck.ecos.records3.record.operation.query.lang.exception;

import ru.citeck.ecos.records3.record.exception.RecordsException;

public class LanguageNotSupportedException extends RecordsException {

    private final String language;
    private final String sourceId;

    public LanguageNotSupportedException(String sourceId, String language) {
        super("Language '" + language + "' is not supported by source: '" + sourceId + "'");
        this.sourceId = sourceId;
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public String getSourceId() {
        return sourceId;
    }
}
