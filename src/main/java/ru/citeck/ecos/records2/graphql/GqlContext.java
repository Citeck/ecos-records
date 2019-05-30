package ru.citeck.ecos.records2.graphql;

import ru.citeck.ecos.records2.RecordsService;

import java.util.List;

public class GqlContext {

    private List<?> metaValues;
    private final RecordsService recordsService;

    public GqlContext(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    public List<?> getMetaValues() {
        return metaValues;
    }

    public void setMetaValues(List<?> metaValues) {
        this.metaValues = metaValues;
    }

    public RecordsService getRecordsService() {
        return recordsService;
    }
}
