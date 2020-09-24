package ru.citeck.ecos.records3.source.common.group;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.graphql.meta.value.InnerMetaValue;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records3.record.op.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.meta.schema.resolver.AttContext;
import ru.citeck.ecos.records3.predicate.model.ComposedPredicate;
import ru.citeck.ecos.records3.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.op.query.request.query.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.request.query.RecsQueryRes;

import java.util.*;
import java.util.stream.Collectors;

public class RecordsGroup implements MetaValue {

    public static final String FIELD_PREDICATE = "predicate";
    public static final String FIELD_PREDICATES = "predicates";
    public static final String FIELD_VALUES = "values";
    public static final String FIELD_SUM = "sum";
    public static final String FIELD_COUNT = "count";

    private final Predicate predicate;
    private final RecordsQuery query;
    private final Map<String, ValueWrapper> attributes;

    private final RecordsService recordsService;

    private final String id;

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
    public Object getAttribute(@NotNull String name) {

        switch (name) {
            case FIELD_PREDICATE:
                return predicate;
            case FIELD_PREDICATES:

                if (predicate instanceof ComposedPredicate) {
                    return ((ComposedPredicate) predicate).getPredicates();
                }

                return Collections.singletonList(predicate);

            case FIELD_VALUES:

                SchemaAtt field = AttContext.getCurrentSchemaAtt();

                // todo
                Map<String, String> innerAttributes = Collections.emptyMap();//field.getInnerAttributesMap();
                RecsQueryRes<RecordMeta> records = recordsService.queryRecords(query, innerAttributes);

                return records.getRecords().stream().map(r -> {
                    ObjectData atts = r.getAttributes();
                    atts.set("id", r.getId().toString());
                    return new InnerMetaValue(Json.getMapper().toJson(atts));
                }).collect(Collectors.toList());
            default:
                //nothing
        }

        if (name.equals(FIELD_COUNT)) {

            RecordsQuery countQuery = new RecordsQuery(query);
            countQuery.setGroupBy(null);
            countQuery.setMaxItems(1);
            countQuery.setSkipCount(0);
            RecsQueryRes<RecordRef> records = recordsService.queryRecords(countQuery);

            return records.getTotalCount();
        }

        if (name.startsWith(FIELD_SUM)) {

            String attribute = name.substring(FIELD_SUM.length() + 1, name.length() - 1) + "?num";
            List<String> attributes = Collections.singletonList(attribute);

            RecordsQuery sumQuery = new RecordsQuery(query);
            sumQuery.setGroupBy(null);
            RecsQueryRes<RecordMeta> result = recordsService.queryRecords(sumQuery, attributes);

            double sum = 0.0;
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
