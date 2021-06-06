package ru.citeck.ecos.records2.source.common.group;

import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.InnerMetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.predicate.model.ComposedPredicate;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.atts.value.impl.InnerAttValue;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @deprecated -> RecordsGroup
 */
@Deprecated
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

                RecsQueryRes<RecordAtts> records = recordsService.query(
                    query,
                    field.getInnerAttributesMap(),
                    true
                );

                return records.getRecords().stream().map(r -> {
                    ObjectData atts = r.getAttributes();
                    atts.set("?id", r.getId().toString());
                    return new InnerAttValue(Json.getMapper().toJson(atts));
                }).collect(Collectors.toList());
            default:
                //nothing
        }

        if (name.equals(FIELD_COUNT)) {

            RecordsQuery countQuery = query.copy()
                .withGroupBy(null)
                .withMaxItems(1)
                .withSkipCount(0)
                .build();
            RecsQueryRes<RecordRef> records = recordsService.query(countQuery);

            return records.getTotalCount();
        }

        if (name.startsWith(FIELD_SUM)) {

            String attribute = name.substring(FIELD_SUM.length() + 1, name.length() - 1) + "?num";
            List<String> attributes = Collections.singletonList(attribute);

            RecordsQuery sumQuery = query.copy().withGroupBy(null).build();
            RecsQueryRes<RecordAtts> result = recordsService.query(sumQuery, attributes);

            double sum = 0.0;
            for (RecordAtts record : result.getRecords()) {
                sum += record.getAtt(attribute, 0.0);
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
