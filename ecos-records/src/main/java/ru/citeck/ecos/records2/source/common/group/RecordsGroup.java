package ru.citeck.ecos.records2.source.common.group;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.attributes.Attributes;
import ru.citeck.ecos.records2.graphql.meta.value.InnerMetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.predicate.model.ComposedPredicate;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.util.*;
import java.util.stream.Collectors;

public class RecordsGroup implements MetaValue {

    public static final String FIELD_PREDICATE = "predicate";
    public static final String FIELD_PREDICATES = "predicates";
    public static final String FIELD_VALUES = "values";
    public static final String FIELD_SUM = "sum";
    public static final String FIELD_COUNT = "count";

    private Predicate predicate;
    private RecordsQuery query;
    private Map<String, ValueWrapper> attributes;

    private RecordsService recordsService;

    private String id;

    public RecordsGroup(RecordsQuery query,
                        Map<String, DistinctValue> attributes,
                        Predicate predicate,
                        RecordsService recordsService) {

        this.id = UUID.randomUUID().toString();
        this.query = query;
        this.predicate = predicate;
        this.recordsService = recordsService;

        this.attributes = new HashMap<>();
        attributes.forEach((n, v) -> this.attributes.put(n, new ValueWrapper(v)));
    }

    @Override
    public String getId() {
        return id;
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
            case FIELD_PREDICATES:

                if (predicate instanceof ComposedPredicate) {
                    return ((ComposedPredicate) predicate).getPredicates();
                }

                return Collections.singletonList(predicate);

            case FIELD_VALUES:

                String schema = field.getInnerSchema();
                RecordsQueryResult<RecordMeta> records = recordsService.queryRecords(query, schema);

                return records.getRecords().stream().map(r -> {
                    Attributes atts = r.getAttributes();
                    atts.set("id", r.getId().toString());
                    return new InnerMetaValue(JsonUtils.toJson(atts));
                }).collect(Collectors.toList());
            default:
                //nothing
        }

        if (name.equals(FIELD_COUNT)) {

            RecordsQuery countQuery = new RecordsQuery(query);
            countQuery.setGroupBy(null);
            countQuery.setMaxItems(1);
            countQuery.setSkipCount(0);
            RecordsQueryResult<RecordRef> records = recordsService.queryRecords(countQuery);

            return records.getTotalCount();
        }

        if (name.startsWith(FIELD_SUM)) {

            String attribute = name.substring(FIELD_SUM.length() + 1, name.length() - 1) + "?num";
            List<String> attributes = Collections.singletonList(attribute);

            RecordsQuery sumQuery = new RecordsQuery(query);
            sumQuery.setGroupBy(null);
            RecordsQueryResult<RecordMeta> result = recordsService.queryRecords(sumQuery, attributes);

            Double sum = 0.0;
            for (RecordMeta record : result.getRecords()) {
                sum += record.get(attribute, 0.0);
            }

            return sum;
        }

        return attributes.get(name);
    }

    private static class ValueWrapper implements MetaValue {

        private DistinctValue value;

        ValueWrapper(DistinctValue value) {
            this.value = value;
        }

        @Override
        public String getString() {
            return value.getValue();
        }

        @Override
        public String getDisplayName() {
            return value.getDisplayName();
        }

        @Override
        public String getId() {
            return value.getId();
        }
    }
}
