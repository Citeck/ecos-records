package ru.citeck.ecos.records3.utils;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class RecordsUtils {

    public static RecordsQueryRes<RecordAtts> metaWithDefaultApp(RecordsQueryRes<RecordAtts> queryResult,
                                                                 String appName) {
        if (StringUtils.isBlank(appName)) {
            return queryResult;
        }
        queryResult.setRecords(queryResult.getRecords()
            .stream()
            .map(meta -> meta.withDefaultAppName(appName))
            .collect(Collectors.toList())
        );
        return queryResult;
    }

    public static Map<String, Class<?>> getAttributesClasses(String sourceId,
                                                             Collection<String> attributes,
                                                             Class<?> defaultClass,
                                                             RecordsService recordsService) {

        Map<String, String> attJavaClasses = new HashMap<>();
        for (String attribute : attributes) {
            attJavaClasses.put(attribute, "#" + attribute + "?javaClass");
        }
        RecordRef recordRef = RecordRef.create(sourceId, "");
        RecordAtts javaClasses = recordsService.getAtts(recordRef, attJavaClasses);

        Map<String, Class<?>> result = new HashMap<>();

        String defaultClassStr = defaultClass != null ? defaultClass.getName() : "";

        for (String attribute : attributes) {

            String javaClassStr = javaClasses.get(attribute, defaultClassStr);
            if (!javaClassStr.isEmpty()) {
                try {
                    result.put(attribute, Class.forName(javaClassStr));
                } catch (ClassNotFoundException e) {
                    log.warn("Attribute class not found: " + javaClassStr, e);
                }
            }
        }

        return result;
    }

    public static List<RecordRef> toLocalRecords(Collection<RecordRef> records) {
        return records.stream()
                      .map(r -> RecordRef.valueOf(r.getId()))
                      .collect(Collectors.toList());
    }

    public static List<RecordAtts> toScopedRecordsMeta(String sourceId, List<RecordAtts> records) {
        if (StringUtils.isBlank(sourceId)) {
            return records;
        }
        return records.stream()
                      .map(n -> new RecordAtts(RecordRef.valueOf(sourceId + "@" + n.getId()), n.getAttributes()))
                      .collect(Collectors.toList());
    }

    public static RecordsQueryRes<RecordRef> toScoped(String sourceId, RecordsQueryRes<RecordRef> result) {
        return new RecordsQueryRes<>(result, r -> RecordRef.create(sourceId, r));
    }

    public static List<RecordRef> toScopedRecords(String sourceId, List<RecordRef> records) {
        return records.stream()
                      .map(r -> RecordRef.create(sourceId, r))
                      .collect(Collectors.toList());
    }

    public static List<RecordRef> strToRecords(String sourceId, List<String> records) {
        return records.stream()
                      .map(r -> RecordRef.create(sourceId, r))
                      .collect(Collectors.toList());
    }

    public static Map<String, List<ValueWithIdx<RecordRef>>> groupRefBySource(Collection<RecordRef> records) {
        return groupBySource(records, r -> r, RecordsUtils::getSourceWithApp, (r, d) -> r);
    }

    public static Map<String, List<ValueWithIdx<RecordRef>>> groupRefBySourceWithIdx(
                                                                    Collection<ValueWithIdx<RecordRef>> records) {
        return groupBySourceWithIdx(records, r -> r, RecordsUtils::getSourceWithApp, (r, d) -> r);
    }

    public static Map<String, List<ValueWithIdx<RecordAtts>>> groupMetaBySource(Collection<RecordAtts> records) {
        return groupBySource(records, RecordAtts::getId, RecordsUtils::getSourceWithApp, (r, d) -> d);
    }

    public static <V> Map<RecordRef, V> convertToRefs(Map<String, V> data) {
        Map<RecordRef, V> result = new HashMap<>();
        data.forEach((id, recMeta) -> result.put(RecordRef.valueOf(id), recMeta));
        return result;
    }

    public static <V> Map<RecordRef, V> convertToRefs(String sourceId, Map<String, V> data) {
        Map<RecordRef, V> result = new HashMap<>();
        data.forEach((id, recMeta) -> result.put(RecordRef.create(sourceId, id), recMeta));
        return result;
    }

    public static  List<RecordAtts> convertToRefs(String sourceId, List<RecordAtts> data) {
        return toScopedRecordsMeta(sourceId, data);
    }

    public static Map<String, List<ValueWithIdx<RecordRef>>> groupByApp(Collection<RecordRef> records) {
        return groupBySource(records, r -> r, RecordRef::getAppName, (r, o) -> r);
    }

    public static Map<String, List<ValueWithIdx<RecordAtts>>> groupAttsByApp(Collection<RecordAtts> records) {
        return groupBySource(records, RecordAtts::getId, RecordRef::getAppName, (r, o) -> o);
    }

    private static String getSourceWithApp(RecordRef recordRef) {
        String appName = recordRef.getAppName();
        String sourceId = recordRef.getSourceId();
        return StringUtils.isNotBlank(appName) ? appName + "/" + sourceId : sourceId;
    }

    private static <I, O> Map<String, List<ValueWithIdx<O>>> groupBySource(Collection<I> records,
                                                                           Function<I, RecordRef> getRecordRef,
                                                                           Function<RecordRef, String> getGroupKey,
                                                                           BiFunction<RecordRef, I, O> toOutput) {

        Map<String, List<ValueWithIdx<O>>> result = new HashMap<>();
        int idx = 0;
        for (I recordData : records) {
            RecordRef record = getRecordRef.apply(recordData);
            String groupKey = getGroupKey.apply(record);
            List<ValueWithIdx<O>> outList = result.computeIfAbsent(groupKey, key -> new ArrayList<>());
            outList.add(new ValueWithIdx<>(toOutput.apply(record, recordData),  idx++));
        }
        return result;
    }

    private static <I, O> Map<String, List<ValueWithIdx<O>>> groupBySourceWithIdx(
                                                                           Collection<ValueWithIdx<I>> records,
                                                                           Function<I, RecordRef> getRecordRef,
                                                                           Function<RecordRef, String> getGroupKey,
                                                                           BiFunction<RecordRef, I, O> toOutput) {

        Map<String, List<ValueWithIdx<O>>> result = new HashMap<>();
        for (ValueWithIdx<I> recordData : records) {
            RecordRef record = getRecordRef.apply(recordData.getValue());
            String groupKey = getGroupKey.apply(record);
            List<ValueWithIdx<O>> outList = result.computeIfAbsent(groupKey, key -> new ArrayList<>());
            outList.add(new ValueWithIdx<>(toOutput.apply(record, recordData.getValue()),  recordData.getIdx()));
        }
        return result;
    }

    public static List<RecordRef> toRecords(Collection<String> strRecords) {
        return strRecords.stream().map(RecordRef::valueOf).collect(Collectors.toList());
    }
}
