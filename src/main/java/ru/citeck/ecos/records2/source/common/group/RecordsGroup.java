package ru.citeck.ecos.records2.source.common.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.ComposedPredicate;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

import java.util.*;
import java.util.stream.Collectors;

public class RecordsGroup implements MetaValue {

    public static final String FIELD_PREDICATE = "predicate";
    public static final String FIELD_PREDICATES = "predicates";
    public static final String FIELD_VALUES = "values";
    public static final String FIELD_SUM = "sum";

    private Predicate predicate;
    private RecordsQuery query;
    private Map<String, ValueWrapper> attributes;

    private RecordsService recordsService;

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
                    ObjectNode atts = r.getAttributes();
                    atts.put("id", r.getId().toString());
                    return new GroupValue(atts);
                }).collect(Collectors.toList());
            default:
                //nothing
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

    public static class GroupValue implements MetaValue {

        private final JsonNode value;

        GroupValue(JsonNode value) {
            this.value = value;
        }

        @Override
        public Object getAttribute(String name, MetaField field) {

            String fieldName = field.getAlias();
            if (fieldName == null) {
                fieldName = field.getName();
            }

            if (fieldName != null) {

                JsonNode node = value.path(fieldName);

                if (node instanceof ArrayNode) {
                    List<Object> result = new ArrayList<>();
                    for (JsonNode val : node) {
                        result.add(new GroupValue(val));
                    }
                    return result;
                } else {
                    return new GroupValue(node);
                }
            }

            return null;
        }

        @Override
        public String getString() {
            return value.path("str").asText();
        }

        @Override
        public String getId() {
            return value.path("id").asText();
        }

        @Override
        public Double getDouble() {
            return value.path("num").asDouble();
        }

        @Override
        public Boolean getBool() {
            return value.path("bool").asBoolean();
        }

        @Override
        public Object getJson() {
            return value.get("json");
        }
    }
}
