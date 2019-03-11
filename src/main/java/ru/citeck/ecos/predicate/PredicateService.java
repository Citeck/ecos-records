package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.type.Predicate;

public interface PredicateService {

    boolean canConvertTo(String language);

    JsonNode convert(String language, ObjectNode predicate);

    JsonNode convert(String language, Predicate predicate);

    void register(PredicateConverter converter);
}
