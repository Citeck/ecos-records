package ru.citeck.ecos.records2.request.query.page;

import ecos.com.fasterxml.jackson210.annotation.JsonCreator;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;

public abstract class QueryPage {

    private static final String SKIP_COUNT_FIELD = "skipCount";
    private static final String MAX_ITEMS_FIELD = "maxItems";
    private static final String AFTER_ID_FIELD = "afterId";

    private final int maxItems;

    QueryPage() {
        maxItems = -1;
    }

    QueryPage(Integer maxItems) {
        this.maxItems = maxItems != null ? maxItems : -1;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public abstract QueryPage withMaxItems(Integer maxItems);

    @JsonCreator
    @com.fasterxml.jackson.annotation.JsonCreator
    public static QueryPage createPage(Object pageDataObj) {

        JsonNode pageData = Json.getMapper().toJson(pageDataObj);

        Integer maxItems = getInt(pageData, MAX_ITEMS_FIELD);
        Integer skipCount = getInt(pageData, SKIP_COUNT_FIELD);

        if (pageData.has(AFTER_ID_FIELD) && skipCount == null) {
            JsonNode afterIdNode = pageData.path(AFTER_ID_FIELD);
            return new AfterPage(RecordRef.valueOf(afterIdNode.textValue()), maxItems);
        } else {
            return new SkipPage(skipCount, maxItems);
        }
    }

    private static Integer getInt(JsonNode node, String field) {
        JsonNode intNode = node.path(field);
        return intNode.canConvertToInt() ? intNode.intValue() : null;
    }
}
