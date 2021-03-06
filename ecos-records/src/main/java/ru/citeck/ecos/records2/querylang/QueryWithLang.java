package ru.citeck.ecos.records2.querylang;

public class QueryWithLang {

    private final Object query;
    private final String language;

    QueryWithLang(Object query, String language) {
        this.query = query;
        this.language = language;
    }

    public Object getQuery() {
        return query;
    }

    public String getLanguage() {
        return language;
    }
}
