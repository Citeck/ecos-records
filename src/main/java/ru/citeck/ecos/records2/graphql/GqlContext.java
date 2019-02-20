package ru.citeck.ecos.records2.graphql;

import java.util.List;

public class GqlContext {

    private List<?> metaValues;

    public List<?> getMetaValues() {
        return metaValues;
    }

    public void setMetaValues(List<?> metaValues) {
        this.metaValues = metaValues;
    }
}
