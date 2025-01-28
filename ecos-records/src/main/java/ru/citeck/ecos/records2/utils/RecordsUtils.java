package ru.citeck.ecos.records2.utils;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class RecordsUtils {

    public static RecsQueryRes<RecordAtts> attsWithDefaultApp(RecsQueryRes<RecordAtts> queryResult, String appName) {
        return attsWithDefaultApp(queryResult, appName, "");
    }

    public static RecsQueryRes<RecordAtts> attsWithDefaultApp(RecsQueryRes<RecordAtts> queryResult,
                                                              String appName, String sourceId) {

        queryResult.setRecords(attsWithDefaultApp(queryResult.getRecords(), appName, sourceId));
        return queryResult;
    }

    public static List<RecordAtts> attsWithDefaultApp(List<RecordAtts> records, String appName) {
        return attsWithDefaultApp(records, appName, "");
    }

    public static List<RecordAtts> attsWithDefaultApp(List<RecordAtts> records, String appName, String sourceId) {
        if (StringUtils.isBlank(appName) && StringUtils.isBlank(sourceId)) {
            return records;
        }
        Stream<RecordAtts> recordsStream = records.stream();
        if (StringUtils.isNotBlank(sourceId)) {
            recordsStream = recordsStream.map(meta -> {
                EntityRef ref = meta.getId();
                if (StringUtils.isBlank(ref.getSourceId())) {
                    ref = EntityRef.create(ref.getAppName(), sourceId, ref.getLocalId());
                    return new RecordAtts(ref, meta.getAtts());
                }
                return meta;
            });
        }
        if (StringUtils.isNotBlank(appName)) {
            recordsStream = recordsStream.map(meta -> meta.withDefaultAppName(appName));
        }
        return recordsStream.collect(Collectors.toList());
    }

    public static Map<String, Class<?>> getAttributesClasses(String sourceId,
                                                             Collection<String> attributes,
                                                             Class<?> defaultClass,
                                                             RecordsService recordsService) {

        Map<String, String> attJavaClasses = new HashMap<>();
        for (String attribute : attributes) {
            attJavaClasses.put(attribute, "#" + attribute + "?javaClass");
        }
        EntityRef recordRef = EntityRef.create(sourceId, "");
        RecordAtts javaClasses = recordsService.getAtts(recordRef, attJavaClasses);

        Map<String, Class<?>> result = new HashMap<>();

        String defaultClassStr = defaultClass != null ? defaultClass.getName() : "";

        for (String attribute : attributes) {

            String javaClassStr = javaClasses.getAtt(attribute, defaultClassStr);
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

    public static List<EntityRef> toLocalRecords(Collection<EntityRef> records) {
        return records.stream()
                      .map(r -> EntityRef.EMPTY.withId(r.getLocalId()))
                      .collect(Collectors.toList());
    }

    public static List<RecordAtts> toScopedRecordsAtts(String sourceId, List<RecordAtts> records) {
        if (StringUtils.isBlank(sourceId)) {
            return records;
        }
        return records.stream()
                      .map(rec -> {
                          EntityRef ref = rec.getId();
                          if (ref.getAppName().isEmpty() && ref.getSourceId().isEmpty()) {
                              new RecordAtts(EntityRef.valueOf(sourceId + "@" + ref), rec.getAtts());
                          }
                          return rec;
                      })
                      .collect(Collectors.toList());
    }

    public static List<EntityRef> strToRecords(String sourceId, List<String> records) {
        return records.stream()
                      .map(r -> EntityRef.create(sourceId, r))
                      .collect(Collectors.toList());
    }

    public static Map<String, List<ValWithIdx<EntityRef>>> groupRefBySource(Collection<EntityRef> records) {
        return groupBySource(records, r -> r, RecordsUtils::getSourceWithApp, (r, d) -> EntityRef.valueOf(r));
    }

    public static Map<String, List<ValWithIdx<EntityRef>>> groupRefBySourceWithIdx(
                                                                    Collection<ValWithIdx<EntityRef>> records) {
        return groupBySourceWithIdx(records, r -> r, RecordsUtils::getSourceWithApp, (r, d) -> r);
    }

    public static Map<String, List<ValWithIdx<RecordAtts>>> groupMetaBySource(Collection<RecordAtts> records) {
        return groupBySource(records, RecordAtts::getId, RecordsUtils::getSourceWithApp, (r, d) -> d);
    }

    public static <V> Map<EntityRef, V> convertToRefs(Map<String, V> data) {
        Map<EntityRef, V> result = new HashMap<>();
        data.forEach((id, recMeta) -> result.put(EntityRef.valueOf(id), recMeta));
        return result;
    }

    public static <V> Map<EntityRef, V> convertToRefs(String sourceId, Map<String, V> data) {
        Map<EntityRef, V> result = new HashMap<>();
        data.forEach((id, recMeta) -> result.put(EntityRef.create(sourceId, id), recMeta));
        return result;
    }

    public static  List<RecordAtts> convertToRefs(String sourceId, List<RecordAtts> data) {
        return toScopedRecordsAtts(sourceId, data);
    }

    public static Map<String, List<ValWithIdx<EntityRef>>> groupByApp(Collection<EntityRef> records) {
        return groupBySource(records, r -> r, EntityRef::getAppName, (r, o) -> EntityRef.valueOf(r));
    }

    public static Map<String, List<ValWithIdx<EntityRef>>> entityGroupByApp(Collection<EntityRef> records) {
        return groupBySource(records, r -> r, EntityRef::getAppName, (r, o) -> r);
    }

    public static Map<String, List<ValWithIdx<RecordAtts>>> groupAttsByApp(Collection<RecordAtts> records) {
        return groupBySource(records, RecordAtts::getId, EntityRef::getAppName, (r, o) -> o);
    }

    private static String getSourceWithApp(EntityRef recordRef) {
        String appName = recordRef.getAppName();
        String sourceId = recordRef.getSourceId();
        return StringUtils.isNotBlank(appName) ? appName + "/" + sourceId : sourceId;
    }

    private static <I, O> Map<String, List<ValWithIdx<O>>> groupBySource(Collection<I> records,
                                                                         Function<I, EntityRef> getRecordRef,
                                                                         Function<EntityRef, String> getGroupKey,
                                                                         BiFunction<EntityRef, I, O> toOutput) {

        Map<String, List<ValWithIdx<O>>> result = new HashMap<>();
        int idx = 0;
        for (I recordData : records) {
            EntityRef record = getRecordRef.apply(recordData);
            String groupKey = getGroupKey.apply(record);
            List<ValWithIdx<O>> outList = result.computeIfAbsent(groupKey, key -> new ArrayList<>());
            outList.add(new ValWithIdx<>(toOutput.apply(record, recordData),  idx++));
        }
        return result;
    }

    private static <I, O> Map<String, List<ValWithIdx<O>>> groupBySourceWithIdx(
                                                                           Collection<ValWithIdx<I>> records,
                                                                           Function<I, EntityRef> getRecordRef,
                                                                           Function<EntityRef, String> getGroupKey,
                                                                           BiFunction<EntityRef, I, O> toOutput) {

        Map<String, List<ValWithIdx<O>>> result = new HashMap<>();
        for (ValWithIdx<I> recordData : records) {
            EntityRef record = getRecordRef.apply(recordData.getValue());
            String groupKey = getGroupKey.apply(record);
            List<ValWithIdx<O>> outList = result.computeIfAbsent(groupKey, key -> new ArrayList<>());
            outList.add(new ValWithIdx<>(toOutput.apply(record, recordData.getValue()),  recordData.getIdx()));
        }
        return result;
    }

    public static List<EntityRef> toRecords(Collection<String> strRecords) {
        return strRecords.stream().map(EntityRef::valueOf).collect(Collectors.toList());
    }
}
