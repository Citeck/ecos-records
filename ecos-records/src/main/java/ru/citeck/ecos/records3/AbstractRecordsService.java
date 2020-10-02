package ru.citeck.ecos.records3;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.JsonMapper;
import ru.citeck.ecos.records3.record.operation.delete.DelStatus;
import ru.citeck.ecos.records3.record.operation.query.QueryConstants;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.utils.AttUtils;

import java.util.*;
import java.util.function.Function;

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
    public Optional<RecordRef> queryOne(RecordsQuery query) {
        return query(query).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public <T> Optional<T> queryOne(RecordsQuery query, Class<T> metaClass) {
        return query(query, metaClass).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public Optional<RecordAtts> queryOne(RecordsQuery query, Map<String, String> attributes) {
        return query(query, attributes).getRecords().stream().findFirst();
    }


    @NotNull
    @Override
    public Optional<RecordAtts> queryOne(RecordsQuery query, Map<String, String> attributes, boolean rawAtts) {
        return query(query, attributes, rawAtts).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public Optional<RecordAtts> queryOne(RecordsQuery query, Collection<String> attributes) {
        return query(query, attributes).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public Optional<RecordAtts> queryOne(RecordsQuery query, Collection<String> attributes, boolean rawAtts) {
        return query(query, attributes, rawAtts).getRecords().stream().findFirst();
    }

    @NotNull
    @Override
    public RecordsQueryRes<RecordAtts> query(RecordsQuery query, Collection<String> attributes) {
        return query(query, AttUtils.toMap(attributes));
    }

    @NotNull
    @Override
    public RecordsQueryRes<RecordAtts> query(RecordsQuery query, Map<String, String> attributes) {
        return query(query, attributes, false);
    }

    @NotNull
    @Override
    public RecordsQueryRes<RecordAtts> query(RecordsQuery query,
                                             Collection<String> attributes,
                                             boolean rawAtts) {

        return query(query, AttUtils.toMap(attributes), rawAtts);
    }

    @NotNull
    @Override
    public RecordsQueryRes<List<RecordRef>> query(List<DataValue> foreach, RecordsQuery query) {
        return queryForEach(foreach, query, this::query);
    }

    @NotNull
    @Override
    public <T> RecordsQueryRes<List<T>> query(List<DataValue> foreach,
                                              RecordsQuery query,
                                              Class<T> metaClass) {

        return queryForEach(foreach, query, q -> query(q, metaClass));
    }

    @NotNull
    @Override
    public RecordsQueryRes<List<RecordAtts>> query(List<DataValue> foreach,
                                                   RecordsQuery query,
                                                   Collection<String> attributes) {

        return queryForEach(foreach, query, q -> query(q, attributes));
    }


    @NotNull
    @Override
    public RecordsQueryRes<List<RecordAtts>> query(List<DataValue> foreach,
                                                   RecordsQuery query,
                                                   Collection<String> attributes,
                                                   boolean rawAtts) {

        return queryForEach(foreach, query, q -> query(q, attributes, rawAtts));
    }

    @NotNull
    @Override
    public RecordsQueryRes<List<RecordAtts>> query(List<DataValue> foreach,
                                                   RecordsQuery query,
                                                   Map<String, String> attributes) {

        return queryForEach(foreach, query, q -> query(q, attributes));
    }

    @NotNull
    @Override
    public RecordsQueryRes<List<RecordAtts>> query(List<DataValue> foreach,
                                                   RecordsQuery query,
                                                   Map<String, String> attributes,
                                                   boolean rawAtts) {

        return queryForEach(foreach, query, q -> query(q, attributes, rawAtts));
    }

    private <T> RecordsQueryRes<List<T>> queryForEach(List<DataValue> foreach,
                                                      RecordsQuery query,
                                                      Function<RecordsQuery, RecordsQueryRes<T>> queryImpl) {

        return RequestContext.withCtx(serviceFactory, context -> {

            RecordsQueryRes<List<T>> result = new RecordsQueryRes<>();

            for (DataValue eachIt : foreach) {

                context.addMsg(MsgLevel.DEBUG, () -> "Start forEach: '" + eachIt + "'");

                RecordsQuery eachRecordsQuery = new RecordsQuery(query);
                eachRecordsQuery.setQuery(replaceIt(mapper.toJson(query.getQuery()), mapper.toJson(eachIt)));
                RecordsQueryRes<T> eachRes = queryImpl.apply(eachRecordsQuery);

                context.addMsg(MsgLevel.DEBUG, () -> "ForEach: '" + eachIt + "' res: " + eachRes);

                result.setTotalCount(result.getTotalCount() + eachRes.getTotalCount());
                result.addRecord(eachRes.getRecords());
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
                newObject.set(name, replaceIt(query.get(name), value));
            }
            return newObject;
        }

        return query;
    }

    /* ATTRIBUTES */

    @NotNull
    @Override
    public DataValue getAtt(Object record, String attribute) {

        if (record == null) {
            return DataValue.NULL;
        }

        List<RecordAtts> meta = getAtts(Collections.singletonList(record),
            Collections.singletonList(attribute));
        if (!meta.isEmpty()) {
            return meta.get(0).getAttribute(attribute);
        }
        return DataValue.NULL;
    }

    @NotNull
    @Override
    public List<RecordAtts> getAtts(Collection<?> records, Collection<String> attributes) {
        return getAtts(records, AttUtils.toMap(attributes), false);
    }

    @NotNull
    @Override
    public List<RecordAtts> getAtts(Collection<?> records, Collection<String> attributes, boolean rawAtts) {
        return getAtts(records, AttUtils.toMap(attributes), rawAtts);
    }

    @NotNull
    @Override
    public RecordAtts getAtts(Object record, Collection<String> attributes) {
        return getAtts(Collections.singletonList(record), attributes, false).get(0);
    }

    @NotNull
    @Override
    public RecordAtts getAtts(Object record, Collection<String> attributes, boolean rawAtts) {
        return getAtts(Collections.singletonList(record), attributes, rawAtts).get(0);
    }

    @NotNull
    @Override
    public RecordAtts getAtts(Object record, Map<String, String> attributes) {
        return getAtts(Collections.singletonList(record), attributes, false).get(0);
    }

    @NotNull
    @Override
    public RecordAtts getAtts(Object record, Map<String, String> attributes, boolean rawAtts) {
        return getAtts(Collections.singletonList(record), attributes, rawAtts).get(0);
    }

    @NotNull
    @Override
    public List<RecordAtts> getAtts(Collection<?> records, Map<String, String> attributes) {
        return getAtts(records, attributes, false);
    }

    /* MUTATE */

    @NotNull
    @Override
    public RecordRef mutate(RecordRef record, String attribute, Object value) {
        return mutate(record, Collections.singletonMap(attribute, value));
    }

    @NotNull
    @Override
    public RecordRef mutate(RecordRef record, Map<String, Object> attributes) {
        return mutate(record, ObjectData.create(attributes));
    }

    @NotNull
    @Override
    public RecordRef mutate(RecordRef record, ObjectData attributes) {
        return mutate(new RecordAtts(record, attributes));
    }

    @NotNull
    @Override
    public RecordRef mutate(RecordAtts meta) {

        List<RecordAtts> records = Collections.singletonList(meta);
        List<RecordRef> recordRefs = this.mutate(records);

        if (recordRefs.size() != 1) {
            log.warn("Strange behaviour. Expected 1 record, but found " + recordRefs.size());
        }

        return recordRefs.get(0);
    }

    /* DELETE */

    @NotNull
    @Override
    public DelStatus delete(RecordRef record) {

        List<DelStatus> result = delete(Collections.singletonList(record));

        if (result.size() != 1) {
            log.warn("Strange behaviour. Expected 1 record, but found " + result.size());
        }

        return result.get(0);
    }
}
