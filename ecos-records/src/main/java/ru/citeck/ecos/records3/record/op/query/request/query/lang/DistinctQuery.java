package ru.citeck.ecos.records3.record.op.query.request.query.lang;

import ru.citeck.ecos.commons.json.Json;

public class DistinctQuery {

    public static final String LANGUAGE = "distinct";

    private String attribute;
    private Object query;
    private String language;

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public Object getQuery() {
        return query;
    }

    public void setQuery(Object query) {
        if (query instanceof String) {
            this.query = query;
        } else {
            this.query = Json.getMapper().toJson(query);
        }
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public static DistinctQuery create(String attribute, Object query, String language) {
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
