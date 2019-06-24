package ru.citeck.ecos.querylang;

import com.fasterxml.jackson.databind.JsonNode;

public interface QueryLangConverter {

    JsonNode convert(JsonNode query);
}
