package ru.citeck.ecos.records2.exception;

public class LanguageNotSupportedException extends RecordsException {

    private String language;
    private String sourceId;

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
