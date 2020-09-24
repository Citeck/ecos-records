package ru.citeck.ecos.records3;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.JsonMapper;
import ru.citeck.ecos.records3.record.operation.query.QueryConstants;
import ru.citeck.ecos.records3.record.operation.query.QueryContext;
import ru.citeck.ecos.records3.record.error.ErrorUtils;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutResult;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutation;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractRecordsService implements RecordsService {

    protected RecordsServiceFactory serviceFactory;

    private final JsonMapper mapper = Json.getMapper();

    AbstractRecordsService(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    /* QUERY */

    @NotNull
    @Override
    public Optional<RecordRef> queryRecord(RecordsQuery query) {
        return queryRecords(query).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public <T> Optional<T> queryRecord(RecordsQuery query, Class<T> metaClass) {
        return queryRecords(query, metaClass).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public Optional<RecordMeta> queryRecord(RecordsQuery query, Map<String, String> attributes) {
        return queryRecords(query, attributes).getRecords().stream().findFirst();
    }


    @NotNull
    @Override
    public Optional<RecordMeta> queryRecord(RecordsQuery query, Map<String, String> attributes, boolean rawAtts) {
        return queryRecords(query, attributes, rawAtts).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public Optional<RecordMeta> queryRecord(RecordsQuery query, Collection<String> attributes) {
        return queryRecords(query, attributes).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public Optional<RecordMeta> queryRecord(RecordsQuery query, Collection<String> attributes, boolean rawAtts) {
        return queryRecords(query, attributes, rawAtts).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public RecsQueryRes<RecordMeta> queryRecords(RecordsQuery query, Collection<String> attributes) {
        return queryRecords(query, toAttributesMap(attributes));
    }

    @NotNull
    @Override
    public RecsQueryRes<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes) {
        return queryRecords(query, attributes, false);
    }

    @NotNull
    @Override
    public RecsQueryRes<RecordMeta> queryRecords(RecordsQuery query,
                                                 Collection<String> attributes,
                                                 boolean rawAtts) {

        return queryRecords(query, toAttributesMap(attributes), rawAtts);
    }

    @NotNull
    @Override
    public RecsQueryRes<List<RecordRef>> queryRecords(List<DataValue> foreach, RecordsQuery query) {
        return queryForEach(foreach, query, this::queryRecords);
    }

    @NotNull
    @Override
    public <T> RecsQueryRes<List<T>> queryRecords(List<DataValue> foreach,
                                                  RecordsQuery query,
                                                  Class<T> metaClass) {

        return queryForEach(foreach, query, q -> queryRecords(q, metaClass));
    }

    @NotNull
    @Override
    public RecsQueryRes<List<RecordMeta>> queryRecords(List<DataValue> foreach,
                                                       RecordsQuery query,
                                                       Collection<String> attributes) {

        return queryForEach(foreach, query, q -> queryRecords(q, attributes));
    }


    @NotNull
    @Override
    public RecsQueryRes<List<RecordMeta>> queryRecords(List<DataValue> foreach,
                                                       RecordsQuery query,
                                                       Collection<String> attributes,
                                                       boolean rawAtts) {

        return queryForEach(foreach, query, q -> queryRecords(q, attributes, rawAtts));
    }

    @NotNull
    @Override
    public RecsQueryRes<List<RecordMeta>> queryRecords(List<DataValue> foreach,
                                                       RecordsQuery query,
                                                       Map<String, String> attributes) {

        return queryForEach(foreach, query, q -> queryRecords(q, attributes));
    }

    @NotNull
    @Override
    public RecsQueryRes<List<RecordMeta>> queryRecords(List<DataValue> foreach,
                                                       RecordsQuery query,
                                                       Map<String, String> attributes,
                                                       boolean rawAtts) {

        return queryForEach(foreach, query, q -> queryRecords(q, attributes, rawAtts));
    }

    private <T> RecsQueryRes<List<T>> queryForEach(List<DataValue> foreach,
                                                   RecordsQuery query,
                                                   Function<RecordsQuery, RecsQueryRes<T>> queryImpl) {

        return QueryContext.withContext(serviceFactory, () -> {

            RecsQueryRes<List<T>> result = new RecsQueryRes<>();

            int idx = 0;

            for (DataValue eachIt : foreach) {

                RecordsQuery eachRecordsQuery = new RecordsQuery(query);
                eachRecordsQuery.setQuery(replaceIt(mapper.toJson(query.getQuery()), mapper.toJson(eachIt)));
                RecsQueryRes<T> eachRes = queryImpl.apply(eachRecordsQuery);

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

    @NotNull
    @Override
    public DataValue getAtt(RecordRef record, String attribute) {

        if (record == null) {
            return DataValue.NULL;
        }

        List<RecordMeta> meta = getAtts(Collections.singletonList(record),
            Collections.singletonList(attribute));
        if (!meta.isEmpty()) {
            return meta.get(0).getAttribute(attribute);
        }
        return DataValue.NULL;
    }

    @NotNull
    @Override
    public List<RecordMeta> getAtts(Collection<RecordRef> records, Collection<String> attributes) {
        return getAtts(records, toAttributesMap(attributes), false);
    }

    @NotNull
    @Override
    public List<RecordMeta> getAtts(Collection<RecordRef> records, Collection<String> attributes, boolean rawAtts) {
        return getAtts(records, toAttributesMap(attributes), rawAtts);
    }

    @NotNull
    @Override
    public RecordMeta getAtts(RecordRef record, Collection<String> attributes) {
        return extractOne(getAtts(Collections.singletonList(record), attributes, false), record);
    }

    @NotNull
    @Override
    public RecordMeta getAtts(RecordRef record, Collection<String> attributes, boolean rawAtts) {
        return extractOne(getAtts(Collections.singletonList(record), attributes, rawAtts), record);
    }

    @NotNull
    @Override
    public RecordMeta getAtts(RecordRef record, Map<String, String> attributes) {
        return extractOne(getAtts(Collections.singletonList(record), attributes, false), record);
    }

    @NotNull
    @Override
    public RecordMeta getAtts(RecordRef record, Map<String, String> attributes, boolean rawAtts) {
        return extractOne(getAtts(Collections.singletonList(record), attributes, rawAtts), record);
    }

    @NotNull
    @Override
    public List<RecordMeta> getAtts(Collection<RecordRef> records, Map<String, String> attributes) {
        return getAtts(records, attributes, false);
    }

    /* MUTATE */

    @NotNull
    @Override
    public RecordMeta mutate(RecordMeta meta) {

        RecordsMutation mutation = new RecordsMutation();
        mutation.addRecord(meta);
        RecordsMutResult result = this.mutate(mutation);

        ErrorUtils.logErrors(result);

        return result.getRecords()
            .stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Record mutation failed. Meta: " + meta));
    }

    /* UTILS */

    private RecordMeta extractOne(List<RecordMeta> values, RecordRef record) {

        if (values.isEmpty()) {
            return new RecordMeta(record);
        }
        RecordMeta meta;
        if (values.size() == 1) {
            meta = values.get(0);
            if (!record.equals(meta.getId())) {
                meta = new RecordMeta(meta, record);
            }
            return meta;
        }

        meta = values.stream()
                     .filter(r -> record.equals(r.getId()))
                     .findFirst()
                     .orElse(null);

        if (meta == null && values.size() > 0) {
            log.warn("Records is not empty but '" + record + "' is not found. Records: "
                + values.stream()
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
