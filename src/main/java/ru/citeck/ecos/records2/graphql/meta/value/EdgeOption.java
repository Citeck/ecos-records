package ru.citeck.ecos.records2.graphql.meta.value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class EdgeOption {

    private String title;
    private JsonNode value;

    public EdgeOption(String title, JsonNode value) {
        this.title = title;
        this.value = value;
    }

    public EdgeOption(String title, String value) {
        this.title = title;
        this.value = TextNode.valueOf(value);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public JsonNode getValue() {
        return value;
    }

    public void setValue(JsonNode value) {
        this.value = value;
    }
}
