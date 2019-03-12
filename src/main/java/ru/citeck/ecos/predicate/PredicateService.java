package ru.citeck.ecos.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.conv.PredicateConverter;
import ru.citeck.ecos.predicate.model.Predicate;

public interface PredicateService {

    boolean canConvertTo(String language);

    JsonNode toLanguage(String language, ObjectNode predicate);

    JsonNode toLanguage(String language, Predicate predicate);

    Predicate fromJson(ObjectNode predicateNode);

    Predicate fromJson(String predicateJson);

    ObjectNode toJson(Predicate predicate);

    void register(PredicateConverter converter);
}
