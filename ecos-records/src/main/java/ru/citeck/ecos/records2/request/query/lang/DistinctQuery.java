package ru.citeck.ecos.records2.request.query.lang;

import ecos.com.fasterxml.jackson210.databind.JsonNode;

public class DistinctQuery {

    public static final String LANGUAGE = "distinct";

    private String attribute;
    private JsonNode query;
    private String language;

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public JsonNode getQuery() {
        return query;
    }

    public void setQuery(JsonNode query) {
        this.query = query;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public static DistinctQuery create(String attribute, JsonNode query, String language) {
        DistinctQuery result = new DistinctQuery();
        result.setQuery(query);
        result.setLanguage(language);
        result.setAttribute(attribute);
        return result;
    }

    @Override
    public String toString() {
        return "DistinctQuery{"
            + "attribute='" + attribute + '\''
            + ", query=" + query
            + ", language='" + language + '\''
            + '}';
    }
}
