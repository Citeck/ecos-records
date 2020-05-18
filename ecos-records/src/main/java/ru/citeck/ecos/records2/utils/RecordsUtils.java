package ru.citeck.ecos.records2.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class RecordsUtils {

    public static RecordsResult<RecordMeta> metaWithDefaultApp(RecordsResult<RecordMeta> metaResult,
                                                               String appName) {
        if (StringUtils.isBlank(appName)) {
            return metaResult;
        }
        metaResult.setRecords(metaResult.getRecords()
            .stream()
            .map(meta -> meta.withDefaultAppName(appName))
            .collect(Collectors.toList())
        );
        return metaResult;
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
        RecordMeta javaClasses = recordsService.getAttributes(recordRef, attJavaClasses);

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

    public static List<RecordMeta> toScopedRecordsMeta(String sourceId, List<RecordMeta> records) {
        if (StringUtils.isBlank(sourceId)) {
            return records;
        }
        return records.stream()
                      .map(n -> new RecordMeta(RecordRef.valueOf(sourceId + "@" + n.getId()), n.getAttributes()))
                      .collect(Collectors.toList());
    }

    public static RecordsResult<RecordRef> toScoped(String sourceId, RecordsResult<RecordRef> result) {
        return new RecordsResult<>(result, r -> RecordRef.create(sourceId, r));
    }

    public static RecordsQueryResult<RecordRef> toScoped(String sourceId, RecordsQueryResult<RecordRef> result) {
        return new RecordsQueryResult<>(result, r -> RecordRef.create(sourceId, r));
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

    public static Map<String, List<RecordRef>> groupRefBySource(Collection<RecordRef> records) {
        return groupBySource(records, r -> r, (r, d) -> r);
    }

    public static Map<String, List<RecordMeta>> groupMetaBySource(Collection<RecordMeta> records) {
        return groupBySource(records, RecordMeta::getId, (r, d) -> d);
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

    public static  List<RecordMeta> convertToRefs(String sourceId, List<RecordMeta> data) {
        return toScopedRecordsMeta(sourceId, data);
    }

    private static <I, O> Map<String, List<O>> groupBySource(Collection<I> records,
                                                             Function<I, RecordRef> getRecordRef,
                                                             BiFunction<RecordRef, I, O> toOutput) {
        Map<String, List<O>> result = new HashMap<>();
        for (I recordData : records) {
            RecordRef record = getRecordRef.apply(recordData);
            String appName = record.getAppName();
            String sourceId = record.getSourceId();
            String sourceWithApp = StringUtils.isNotBlank(appName) ? appName + "/" + sourceId : sourceId;
            List<O> outList = result.computeIfAbsent(sourceWithApp, key -> new ArrayList<>());
            outList.add(toOutput.apply(record, recordData));
        }
        return result;
    }

    public static List<RecordRef> toRecords(Collection<String> strRecords) {
        return strRecords.stream().map(RecordRef::valueOf).collect(Collectors.toList());
    }
}
