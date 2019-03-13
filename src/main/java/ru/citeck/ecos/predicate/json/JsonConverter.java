package ru.citeck.ecos.predicate.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.Predicate;

public interface JsonConverter {

    Predicate fromJson(ObjectNode node);

    ObjectNode toJson(Predicate predicate);
}
