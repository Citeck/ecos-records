package ru.citeck.ecos.records2.source.common.group;

import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;

public class DistinctValue {

    private String id;

    @MetaAtt(".str")
    private String value;

    @MetaAtt(".disp")
    private String displayName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
