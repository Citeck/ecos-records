package ru.citeck.ecos.records3.source.common.group;

import ru.citeck.ecos.records3.graphql.meta.annotation.AttName;

public class DistinctValue {

    private String id;

    @AttName(".str")
    private String value;

    @AttName(".disp")
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
