package ru.citeck.ecos.records2.meta;

import java.util.Map;

public class AttributesSchema {

    private String schema;
    private Map<String, String> keysMapping;

    public AttributesSchema(String schema, Map<String, String> keysMapping) {
        this.schema = schema;
        this.keysMapping = keysMapping;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Map<String, String> getKeysMapping() {
        return keysMapping;
    }

    public void setKeysMapping(Map<String, String> keysMapping) {
        this.keysMapping = keysMapping;
    }
}
