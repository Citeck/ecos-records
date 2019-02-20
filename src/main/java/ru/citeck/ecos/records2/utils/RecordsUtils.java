package ru.citeck.ecos.records2.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecordsUtils {

    private static final Log logger = LogFactory.getLog(RecordsUtils.class);

    public static RecordRef getRecordId(ObjectNode recordMeta) {
        JsonNode idNode = recordMeta.get("id");
        String id = idNode != null && idNode.isTextual() ? idNode.asText() : null;
        return id != null ? new RecordRef(id) : null;
    }

    public static List<RecordRef> toLocalRecords(Collection<RecordRef> records) {
        return records.stream()
                      .map(r -> new RecordRef(r.getId()))
                      .collect(Collectors.toList());
    }

    public static String getMetaValueId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof MetaValue) {
            return ((MetaValue) value).getId();
        }
        try {
            Object propValue = PropertyUtils.getProperty(value, "id");
            return propValue != null ? propValue.toString() : null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            logger.error(e);
        }
        return null;
    }

    public static List<RecordMeta> toScopedRecordsMeta(String sourceId, List<RecordMeta> records) {
        if (StringUtils.isBlank(sourceId)) {
            return records;
        }
        return records.stream()
                      .map(n -> new RecordMeta(new RecordRef(sourceId, n.getId()), n.getAttributes()))
                      .collect(Collectors.toList());
    }

    public static RecordsResult<RecordRef> toScoped(String sourceId, RecordsResult<RecordRef> result) {
        return new RecordsResult<>(result, r -> new RecordRef(sourceId, r));
    }

    public static RecordsQueryResult<RecordRef> toScoped(String sourceId, RecordsQueryResult<RecordRef> result) {
        return new RecordsQueryResult<>(result, r -> new RecordRef(sourceId, r));
    }

    public static List<RecordRef> toScopedRecords(String sourceId, List<RecordRef> records) {
        return records.stream()
                      .map(r -> new RecordRef(sourceId, r))
                      .collect(Collectors.toList());
    }

    public static List<RecordRef> strToRecords(String sourceId, List<String> records) {
        return records.stream()
                      .map(r -> new RecordRef(sourceId, r))
                      .collect(Collectors.toList());
    }

    public static Map<String, List<RecordRef>> groupRefBySource(Collection<RecordRef> records) {
        return groupBySource(records, r -> r, (r, d) -> r);
    }

    public static Map<String, List<RecordMeta>> groupMetaBySource(Collection<RecordMeta> records) {
        return groupBySource(records, RecordMeta::getId, (r, d) -> d);
    }

    public static <V> Map<RecordRef, V> convertToRefs(Map<String, V> data) {
        Map<RecordRef, V> result = new HashMap<>();
        data.forEach((id, recMeta) -> result.put(new RecordRef(id), recMeta));
        return result;
    }

    public static <V> Map<RecordRef, V> convertToRefs(String sourceId, Map<String, V> data) {
        Map<RecordRef, V> result = new HashMap<>();
        data.forEach((id, recMeta) -> result.put(new RecordRef(sourceId, id), recMeta));
        return result;
    }

    public static  List<RecordMeta> convertToRefs(String sourceId, List<RecordMeta> data) {
        return toScopedRecordsMeta(sourceId, data);
    }

    private static <I, O> Map<String, List<O>> groupBySource(Collection<I> records,
                                                            Function<I, RecordRef> getRecordRef,
                                                            BiFunction<RecordRef, I, O> toOutput) {
        Map<String, List<O>> result = new HashMap<>();
        for (I recordData : records) {
            RecordRef record = getRecordRef.apply(recordData);
            String sourceId = record.getSourceId();
            List<O> outList = result.computeIfAbsent(sourceId, key -> new ArrayList<>());
            outList.add(toOutput.apply(record, recordData));
        }
        return result;
    }

    public static List<RecordRef> toRecords(Collection<String> strRecords) {
        return strRecords.stream().map(RecordRef::new).collect(Collectors.toList());
    }
}
