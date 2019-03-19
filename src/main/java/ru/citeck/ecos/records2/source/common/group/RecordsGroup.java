package ru.citeck.ecos.records2.source.common.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.citeck.ecos.predicate.model.ComposedPredicate;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.ValuePredicate;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordsGroup implements MetaValue {

    public static final String FIELD_PREDICATE = "predicate";
    public static final String FIELD_PREDICATES = "predicates";
    public static final String FIELD_VALUES = "values";
    public static final String FIELD_SUM = "sum";

    private Predicate predicate;
    private RecordsQuery query;
    private GroupValues values;

    private RecordsService recordsService;

    public RecordsGroup(RecordsQuery query,
                        Predicate predicate,
                        RecordsService recordsService) {

        this.query = query;
        this.recordsService = recordsService;
        this.predicate = predicate;
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
                if (values == null) {
                    String schema = field.getAttributeSchema("records");
                    values = new GroupValues(recordsService.queryRecords(query, schema));
                }
                return values;
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

        return findAttribute(predicate, name);
    }

    private Object findAttribute(Predicate predicate, String attribute) {
        if (predicate instanceof ComposedPredicate) {
            List<Predicate> predicates = ((ComposedPredicate) predicate).getPredicates();
            for (Predicate pred : predicates) {
                Object value = findAttribute(pred, attribute);
                if (value != null) {
                    return value;
                }
            }
        } else if (predicate instanceof ValuePredicate) {
            ValuePredicate predValue = (ValuePredicate) predicate;
            if (attribute.equals(predValue.getAttribute())) {
                return predValue.getValue();
            }
        }
        return null;
    }


    public static class GroupValues implements MetaValue {

        private final RecordsQueryResult<RecordMeta> result;

        public GroupValues(RecordsQueryResult<RecordMeta> result) {
            this.result = result;
        }

        @Override
        public String getString() {
            return null;
        }

        @Override
        public Object getAttribute(String name, MetaField field) {

            switch (name) {
                case "records":

                    List<GroupValue> values = new ArrayList<>();

                    for (RecordMeta meta : result.getRecords()) {
                        ObjectNode attributes = meta.getAttributes();
                        attributes.put("id", meta.getId().toString());
                        values.add(new GroupValue(attributes));
                    }

                    return values;

                case "totalCount":
                    return result.getTotalCount();
                case "hasMore":
                    return result.getHasMore();
                default:
                    return null;
            }
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
