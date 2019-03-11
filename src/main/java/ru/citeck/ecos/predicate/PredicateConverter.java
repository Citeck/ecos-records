package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface PredicateConverter {

    JsonNode convert(ObjectNode predicate);

    String getLanguage();
}
