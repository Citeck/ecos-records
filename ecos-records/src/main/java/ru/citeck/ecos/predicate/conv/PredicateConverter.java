package ru.citeck.ecos.predicate.conv;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ru.citeck.ecos.predicate.model.Predicate;

public interface PredicateConverter {

    JsonNode convert(Predicate predicate);

    String getLanguage();
}
