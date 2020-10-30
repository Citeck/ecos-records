package ru.citeck.ecos.records2.meta;

import java.util.Map;

@Deprecated
public class AttributesSchema {

    private String schema;
    private Map<String, AttSchemaInfo> attsInfo;

    public AttributesSchema(String schema, Map<String, AttSchemaInfo> attsInfo) {
        this.schema = schema;
        this.attsInfo = attsInfo;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Map<String, AttSchemaInfo> getAttsInfo() {
        return attsInfo;
    }

    public void setAttsInfo(Map<String, AttSchemaInfo> attsInfo) {
        this.attsInfo = attsInfo;
    }

}
