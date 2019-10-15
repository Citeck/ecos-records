package ru.citeck.ecos.querylang;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

public interface QueryLangService {

    /**
     * Convert query.
     *
     * @return a query in the toLang language or null if the conversion is not possible
     */
    Optional<JsonNode> convertLang(JsonNode query, String fromLang, String toLang);

    Optional<QueryWithLang> convertLang(JsonNode query, String fromLang, List<String> toLang);

    void register(QueryLangConverter converter, String fromLang, String toLang);
}


