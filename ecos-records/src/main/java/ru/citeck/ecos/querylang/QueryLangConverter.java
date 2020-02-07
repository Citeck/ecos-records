package ru.citeck.ecos.querylang;

import ecos.com.fasterxml.jackson210.databind.JsonNode;

public interface QueryLangConverter {

    JsonNode convert(JsonNode query);
}
