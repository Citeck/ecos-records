package ru.citeck.ecos.records2.predicate.json;

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ru.citeck.ecos.records2.predicate.model.Predicate;

public interface JsonConverter {

    Predicate fromJson(ObjectNode node);

    ObjectNode toJson(Predicate predicate);
}
