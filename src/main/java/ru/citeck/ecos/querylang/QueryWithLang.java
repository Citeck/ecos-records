package ru.citeck.ecos.querylang;

import com.fasterxml.jackson.databind.JsonNode;

public class QueryWithLang {

    private final JsonNode query;
    private final String language;

    QueryWithLang(JsonNode query, String language) {
        this.query = query;
        this.language = language;
    }

    public JsonNode getQuery() {
        return query;
    }

    public String getLanguage() {
        return language;
    }
}
