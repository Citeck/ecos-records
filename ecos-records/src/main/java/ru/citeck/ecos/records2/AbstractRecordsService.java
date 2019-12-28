package ru.citeck.ecos.records2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractRecordsService implements RecordsService {

    protected RecordsServiceFactory serviceFactory;

    AbstractRecordsService(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    /* QUERY */

    @Override
    public Optional<RecordRef> queryRecord(RecordsQuery query) {
        return queryRecords(query).getRecords().stream().findFirst();
    }

    @Override
    public <T> Optional<T> queryRecord(RecordsQuery query, Class<T> metaClass) {
        return queryRecords(query, metaClass).getRecords().stream().findFirst();
    }

    @Override
    public Optional<RecordMeta> queryRecord(RecordsQuery query, Map<String, String> attributes) {
        return queryRecords(query, attributes).getRecords().stream().findFirst();
    }

    @Override
    public Optional<RecordMeta> queryRecord(RecordsQuery query, Collection<String> attributes) {
        return queryRecords(query, attributes).getRecords().stream().findFirst();
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Collection<String> attributes) {
        return queryRecords(query, toAttributesMap(attributes));
    }

    @Override
    public RecordsQueryResult<List<RecordRef>> queryRecords(List<JsonNode> foreach, RecordsQuery query) {
        return queryForEach(foreach, query, this::queryRecords);
    }

    @Override
    public <T> RecordsQueryResult<List<T>> queryRecords(List<JsonNode> foreach,
                                                        RecordsQuery query,
                                                        Class<T> metaClass) {

        return queryForEach(foreach, query, q -> queryRecords(q, metaClass));
    }

    @Override
    public RecordsQueryResult<List<RecordMeta>> queryRecords(List<JsonNode> foreach,
                                                             RecordsQuery query,
                                                             Collection<String> attributes) {

        return queryForEach(foreach, query, q -> queryRecords(q, attributes));
    }

    @Override
    public RecordsQueryResult<List<RecordMeta>> queryRecords(List<JsonNode> foreach,
                                                             RecordsQuery query,
                                                             Map<String, String> attributes) {

        return queryForEach(foreach, query, q -> queryRecords(q, attributes));
    }

    @Override
    public RecordsQueryResult<List<RecordMeta>> queryRecords(List<JsonNode> foreach,
                                                             RecordsQuery query,
                                                             String schema) {

        return queryForEach(foreach, query, q -> queryRecords(q, schema));
    }

    private <T> RecordsQueryResult<List<T>> queryForEach(List<JsonNode> foreach,
                                                         RecordsQuery query,
                                                         Function<RecordsQuery, RecordsQueryResult<T>> queryImpl) {

        return withQueryContext(() -> {

            RecordsQueryResult<List<T>> result = new RecordsQueryResult<>();

            int idx = 0;

            for (JsonNode eachIt : foreach) {

                RecordsQuery eachRecordsQuery = new RecordsQuery(query);
                eachRecordsQuery.setQuery(replaceIt(query.getQuery(), eachIt));
                RecordsQueryResult<T> eachRes = queryImpl.apply(eachRecordsQuery);

                result.setTotalCount(result.getTotalCount() + eachRes.getTotalCount());
                result.addRecord(eachRes.getRecords());
                result.addErrors(eachRes.getErrors());

                if (eachRes.getDebug() != null) {
                    result.setDebugInfo(AbstractRecordsService.class, "each_" + idx, eachRes.getDebug());
                }

                idx++;
            }

            return result;
        });
    }

    private JsonNode replaceIt(JsonNode query, JsonNode value) {

        if (query == null || query.isNull() || query.isMissingNode()) {
            return query;
        }

        if (query.isTextual()) {

            String text = query.asText();
            if (text.equals(QueryConstants.IT_VAR)) {
                return value;
            } else if (text.contains(QueryConstants.IT_VAR)) {
                return TextNode.valueOf(text.replace(QueryConstants.IT_VAR, value.asText()));
            } else {
                return query;
            }
        } else if (query.isArray()) {

            ArrayNode newArray = JsonNodeFactory.instance.arrayNode();
            for (JsonNode node : query) {
                newArray.add(replaceIt(node, value));
            }
            return newArray;

        } else if (query.isObject()) {

            ObjectNode newObject = JsonNodeFactory.instance.objectNode();

            Iterator<String> names = query.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                newObject.put(name, replaceIt(query.get(name), value));
            }
            return newObject;
        }

        return query;
    }

    /* ATTRIBUTES */

    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records,
                                                   Collection<String> attributes) {

        return getAttributes(records, toAttributesMap(attributes));
    }

    @Override
    public RecordMeta getAttributes(RecordRef record, Collection<String> attributes) {
        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordMeta getAttributes(RecordRef record, Map<String, String> attributes) {
        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordMeta getRawAttributes(RecordRef record, Map<String, String> attributes) {
        return extractOne(getRawAttributes(Collections.singletonList(record), attributes), record);
    }

    /* UTILS */

    <T> T withQueryContext(Supplier<T> callable) {

        QueryContext context = QueryContext.getCurrent();
        boolean isContextOwner = false;
        if (context == null) {
            context = serviceFactory.createQueryContext();
            QueryContext.setCurrent(context);
            isContextOwner = true;
        }

        T result;

        try {
            result = callable.get();
        } finally {
            if (isContextOwner) {
                QueryContext.removeCurrent();
            }
        }

        return result;
    }

    private RecordMeta extractOne(RecordsResult<RecordMeta> values, RecordRef record) {

        if (values.getRecords().isEmpty()) {
            return new RecordMeta(record);
        }
        RecordMeta meta;
        if (values.getRecords().size() == 1) {
            meta = values.getRecords().get(0);
            if (!record.equals(meta.getId())) {
                meta = new RecordMeta(meta, record);
            }
            return meta;
        }

        meta = values.getRecords()
                     .stream()
                     .filter(r -> record.equals(r.getId()))
                     .findFirst()
                     .orElse(null);

        if (meta == null && values.getRecords().size() > 0) {
            log.warn("Records is not empty but '" + record + "' is not found. Records: "
                + values.getRecords()
                        .stream()
                        .map(m -> "'" + m.getId() + "'")
                        .collect(Collectors.joining(", ")));
        }
        if (meta == null) {
            meta = new RecordMeta(record);
        }
        return meta;
    }

    private Map<String, String> toAttributesMap(Collection<String> attributes) {
        Map<String, String> attributesMap = new HashMap<>();
        for (String attribute : attributes) {
            attributesMap.put(attribute, attribute);
        }
        return attributesMap;
    }
}
