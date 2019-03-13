package ru.citeck.ecos.predicate.conv;

import com.fasterxml.jackson.databind.JsonNode;
import ru.citeck.ecos.predicate.model.Predicate;

public interface PredicateConverter {

    JsonNode convert(Predicate predicate);

    String getLanguage();
}
