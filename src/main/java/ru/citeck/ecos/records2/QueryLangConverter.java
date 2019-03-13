package ru.citeck.ecos.records2;

import com.fasterxml.jackson.databind.JsonNode;

public interface QueryLangConverter {

    JsonNode convert(JsonNode query);
}
