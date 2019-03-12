package ru.citeck.ecos.records2;

import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

import java.util.Collections;

public class RecordsGroup implements MetaValue {

    public static final String FIELD_PREDICATE = "predicate";
    public static final String FIELD_VALUES = "values";
    public static final String FIELD_SUM = "sum";

    private Predicate predicate;
    private RecordsQuery query;

    private RecordsService recordsService;

    public RecordsGroup(RecordsQuery query,
                        Predicate predicate,
                        RecordsService recordsService) {

        this.query = query;
        this.predicate = predicate;
        this.recordsService = recordsService;
    }

    @Override
    public String getString() {
        return String.valueOf(predicate);
    }

    @Override
    public Object getAttribute(String name, MetaField field) {

        switch (name) {
            case FIELD_PREDICATE:
                return predicate;
            case FIELD_VALUES:
                return recordsService.getRecords(query, field.getInnerSchema());
        }

        if (name.startsWith(FIELD_SUM)) {

            String attribute = name.substring(FIELD_SUM.length() + 1, name.length() - 1) + "?num";
            RecordsQueryResult<RecordMeta> result = recordsService.getRecords(query, Collections.singleton(attribute));

            Double sum = 0.0;
            for (RecordMeta record : result.getRecords()) {
                sum += record.get(attribute, 0.0);
            }

            return sum;
        }

        return null;
    }
}
