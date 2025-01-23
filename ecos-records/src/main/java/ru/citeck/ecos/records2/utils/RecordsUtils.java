package ru.citeck.ecos.records2.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class RecordsUtils {

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
            log.debug("Error", e);
        }
        return null;
    }

    public static RecordsQueryResult<RecordAtts> attsWithDefaultApp(RecordsQueryResult<RecordAtts> queryResult,
                                                                    String appName, String sourceId) {

        queryResult.setRecords(attsWithDefaultApp(queryResult.getRecords(), appName, sourceId));
        return queryResult;
    }

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
                RecordRef ref = meta.getId();
                if (StringUtils.isBlank(ref.getSourceId())) {
                    ref = RecordRef.create(ref.getAppName(), sourceId, ref.getId());
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

    public static RecordsQueryResult<RecordMeta> metaWithDefaultApp(RecordsQueryResult<RecordMeta> queryResult,
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

    public static List<RecordRef> toLocalRecords(Collection<RecordRef> records) {
        return records.stream()
                      .map(r -> RecordRef.EMPTY.withId(r.getLocalId()))
                      .collect(Collectors.toList());
    }

    public static List<RecordAtts> toScopedRecordsAtts(String sourceId, List<RecordAtts> records) {
        if (StringUtils.isBlank(sourceId)) {
            return records;
        }
        return records.stream()
                      .map(rec -> {
                          RecordRef ref = rec.getId();
                          if (ref.getAppName().isEmpty() && ref.getSourceId().isEmpty()) {
                              new RecordAtts(RecordRef.valueOf(sourceId + "@" + ref), rec.getAtts());
                          }
                          return rec;
                      })
                      .collect(Collectors.toList());
    }

    @Deprecated
    public static List<RecordMeta> toScopedRecordsMeta(String sourceId, List<RecordMeta> records) {
        if (StringUtils.isBlank(sourceId)) {
            return records;
        }
        return records.stream()
                      .map(n -> new RecordMeta(RecordRef.valueOf(sourceId + "@" + n.getId()), n.getAttributes()))
                      .collect(Collectors.toList());
    }

    public static RecsQueryRes<RecordRef> toScoped(String sourceId, RecsQueryRes<RecordRef> result) {
        return result.withRecords(recordRef -> RecordRef.create(sourceId, recordRef));
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

    public static Map<String, List<ValWithIdx<RecordRef>>> groupRefBySource(Collection<RecordRef> records) {
        return groupBySource(records, r -> r, RecordsUtils::getSourceWithApp, (r, d) -> RecordRef.valueOf(r));
    }

    public static Map<String, List<ValWithIdx<RecordRef>>> groupRefBySourceWithIdx(
                                                                    Collection<ValWithIdx<RecordRef>> records) {
        return groupBySourceWithIdx(records, r -> r, RecordsUtils::getSourceWithApp, (r, d) -> r);
    }

    public static Map<String, List<ValWithIdx<RecordAtts>>> groupMetaBySource(Collection<RecordAtts> records) {
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

    @Deprecated
    public static  List<RecordMeta> convertToRefsMeta(String sourceId, List<RecordMeta> data) {
        return toScopedRecordsMeta(sourceId, data);
    }

    public static  List<RecordAtts> convertToRefs(String sourceId, List<RecordAtts> data) {
        return toScopedRecordsAtts(sourceId, data);
    }

    public static Map<String, List<ValWithIdx<RecordRef>>> groupByApp(Collection<RecordRef> records) {
        return groupBySource(records, r -> r, EntityRef::getAppName, (r, o) -> RecordRef.valueOf(r));
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
                                                                           Function<I, RecordRef> getRecordRef,
                                                                           Function<RecordRef, String> getGroupKey,
                                                                           BiFunction<RecordRef, I, O> toOutput) {

        Map<String, List<ValWithIdx<O>>> result = new HashMap<>();
        for (ValWithIdx<I> recordData : records) {
            RecordRef record = getRecordRef.apply(recordData.getValue());
            String groupKey = getGroupKey.apply(record);
            List<ValWithIdx<O>> outList = result.computeIfAbsent(groupKey, key -> new ArrayList<>());
            outList.add(new ValWithIdx<>(toOutput.apply(record, recordData.getValue()),  recordData.getIdx()));
        }
        return result;
    }

    public static List<RecordRef> toRecords(Collection<String> strRecords) {
        return strRecords.stream().map(RecordRef::valueOf).collect(Collectors.toList());
    }
}
