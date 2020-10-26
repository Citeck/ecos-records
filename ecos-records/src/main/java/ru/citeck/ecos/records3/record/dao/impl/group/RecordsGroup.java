package ru.citeck.ecos.records3.record.dao.impl.group;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.source.common.group.DistinctValue;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.InnerAttValue;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttContext;
import ru.citeck.ecos.records2.predicate.model.ComposedPredicate;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;

import java.util.*;
import java.util.stream.Collectors;

public class RecordsGroup implements AttValue {

    public static final String FIELD_PREDICATE = "predicate";
    public static final String FIELD_PREDICATES = "predicates";
    public static final String FIELD_VALUES = "values";
    public static final String FIELD_SUM = "sum";
    public static final String FIELD_COUNT = "count";

    private final Predicate predicate;
    private final RecordsQuery query;
    private final Map<String, ValueWrapper> attributes;

    private final RecordsService recordsService;

    public RecordsGroup(RecordsQuery query,
                        Map<String, DistinctValue> attributes,
                        Predicate predicate,
                        RecordsService recordsService) {

        this.query = query;
        this.predicate = predicate;
        this.recordsService = recordsService;

        this.attributes = new HashMap<>();
        attributes.forEach((n, v) -> this.attributes.put(n, new ValueWrapper(v)));
    }

    @Override
    public String asText() {
        return String.valueOf(predicate);
    }

    @Override
    public Object getAtt(@NotNull String name) {

        switch (name) {
            case FIELD_PREDICATE:
                return predicate;
            case FIELD_PREDICATES:

                if (predicate instanceof ComposedPredicate) {
                    return ((ComposedPredicate) predicate).getPredicates();
                }

                return Collections.singletonList(predicate);

            case FIELD_VALUES:

                Map<String, String> innerAttributes = AttContext.getInnerAttsMap();
                RecsQueryRes<RecordAtts> records = recordsService.query(query, innerAttributes);

                return records.getRecords().stream().map(r -> {
                    ObjectData atts = r.getAttributes();
                    atts.set("id", r.getId().toString());
                    return new InnerAttValue(Json.getMapper().toJson(atts));
                }).collect(Collectors.toList());
            default:
                //nothing
        }

        if (name.equals(FIELD_COUNT)) {

            RecordsQuery countQuery = new RecordsQuery(query);
            countQuery.setGroupBy(null);
            countQuery.setMaxItems(1);
            countQuery.setSkipCount(0);
            RecsQueryRes<RecordRef> records = recordsService.query(countQuery);

            return records.getTotalCount();
        }

        if (name.startsWith(FIELD_SUM)) {

            String attribute = name.substring(FIELD_SUM.length() + 1, name.length() - 1) + "?num";
            List<String> attributes = Collections.singletonList(attribute);

            RecordsQuery sumQuery = new RecordsQuery(query);
            sumQuery.setGroupBy(null);
            RecsQueryRes<RecordAtts> result = recordsService.query(sumQuery, attributes);

            double sum = 0.0;
            for (RecordAtts record : result.getRecords()) {
                sum += record.get(attribute, 0.0);
            }

            return sum;
        }

        return attributes.get(name);
    }

    private static class ValueWrapper implements AttValue {

        private final DistinctValue value;

        ValueWrapper(DistinctValue value) {
            this.value = value;
        }

        @Override
        public String asText() {
            return value.getValue();
        }

        @Override
        public String getDispName() {
            return value.getDisplayName();
        }

        @Override
        public String getId() {
            return value.getId();
        }
    }
}
