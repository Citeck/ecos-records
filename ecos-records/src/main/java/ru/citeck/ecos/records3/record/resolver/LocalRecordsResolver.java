package ru.citeck.ecos.records3.record.resolver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.ServiceFactoryAware;
import ru.citeck.ecos.records3.record.operation.delete.DelStatus;
import ru.citeck.ecos.records3.record.operation.delete.RecordsDeleteDao;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.operation.mutate.RecordsMutateDao;
import ru.citeck.ecos.records3.record.operation.query.lang.exception.LanguageNotSupportedException;
import ru.citeck.ecos.records3.record.exception.RecsSourceNotFoundException;
import ru.citeck.ecos.records3.record.operation.meta.RecordAttsService;
import ru.citeck.ecos.records3.predicate.PredicateService;
import ru.citeck.ecos.records3.predicate.model.AndPredicate;
import ru.citeck.ecos.records3.predicate.model.OrPredicate;
import ru.citeck.ecos.records3.predicate.model.Predicate;
import ru.citeck.ecos.records3.predicate.model.Predicates;
import ru.citeck.ecos.records3.record.operation.query.lang.QueryLangService;
import ru.citeck.ecos.records3.record.operation.query.lang.QueryWithLang;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.record.operation.query.lang.DistinctQuery;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.source.info.ColumnsSourceId;
import ru.citeck.ecos.records3.source.common.group.RecordsGroupDao;
import ru.citeck.ecos.records3.source.dao.*;
import ru.citeck.ecos.records3.source.info.RecsDaoFeature;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;
import ru.citeck.ecos.records3.source.dao.local.job.Job;
import ru.citeck.ecos.records3.source.dao.local.job.JobExecutor;
import ru.citeck.ecos.records3.source.dao.local.job.JobsProvider;
import ru.citeck.ecos.records3.utils.RecordsUtils;
import ru.citeck.ecos.records3.utils.ValueWithIdx;

import javax.management.Query;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Slf4j
public class LocalRecordsResolver implements RecordsResolver, RecordsDaoRegistry {

    private static final String DEBUG_QUERY_TIME = "queryTimeMs";
    private static final String DEBUG_META_SCHEMA = "schema";

    private final Map<String, RecordsDao> allDao = new ConcurrentHashMap<>();
    private final Map<String, RecordsAttsDao> attsDao = new ConcurrentHashMap<>();
    private final Map<String, RecordsQueryDao> queryDao = new ConcurrentHashMap<>();
    private final Map<String, RecordsMutateDao> mutateDao = new ConcurrentHashMap<>();
    private final Map<String, RecordsDeleteDao> deleteDao = new ConcurrentHashMap<>();

    private final Map<Class<? extends RecordsDao>, Map<String, ? extends RecordsDao>> daoMapByType;

    private final QueryLangService queryLangService;
    private final RecordsServiceFactory serviceFactory;
    private final RecordAttsService recordsAttsService;

    private final String currentApp;

    private final JobExecutor jobExecutor = new JobExecutor();

    public LocalRecordsResolver(RecordsServiceFactory serviceFactory) {

        this.serviceFactory = serviceFactory;
        this.recordsAttsService = serviceFactory.getRecordsMetaService();
        this.queryLangService = serviceFactory.getQueryLangService();
        this.currentApp = serviceFactory.getProperties().getAppName();

        daoMapByType = new HashMap<>();
        daoMapByType.put(RecordsAttsDao.class, attsDao);
        daoMapByType.put(RecordsQueryDao.class, queryDao);
        daoMapByType.put(RecordsMutateDao.class, mutateDao);
    }

    public void initJobs(ScheduledExecutorService executor) {
        this.jobExecutor.init(executor);
    }

    @Nullable
    @Override
    public RecordsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                             @NotNull Map<String, String> attributes,
                                             boolean rawAtts) {

        RequestContext context = RequestContext.getCurrentNotNull();

        String sourceId = query.getSourceId();
        int appDelimIdx = sourceId.indexOf('/');

        if (appDelimIdx != -1) {

            String appName = sourceId.substring(0, appDelimIdx);

            if (appName.equals(currentApp)) {
                sourceId = sourceId.substring(appDelimIdx + 1);
                query = new RecordsQuery(query);
                query.setSourceId(sourceId);
            }
        }
        RecordsQuery finalQuery = query;

        RecordsQueryRes<RecordAtts> recordsResult = null;

        if (!query.getGroupBy().isEmpty()) {

            RecordsQueryDao dao = getRecordsDao(sourceId, RecordsQueryDao.class).orElse(null);

            if (dao == null || !dao.isGroupingSupported()) {

                RecordsQueryDao groupsSource = needRecordsDao(RecordsGroupDao.ID, RecordsQueryDao.class);

                RecordsQuery convertedQuery = updateQueryLanguage(query, groupsSource);

                if (convertedQuery == null) {

                    String errorMsg = "GroupBy is not supported by language: "
                                      + query.getLanguage() + ". Query: " + query;
                    context.addMsg(MsgLevel.ERROR, () -> errorMsg);

                } else {
                    RecordsQueryRes<?> queryRes = groupsSource.queryRecords(convertedQuery);
                    if (queryRes != null) {
                        List<RecordAtts> atts = recordsAttsService.getAtts(queryRes.getRecords(), attributes, rawAtts);
                        recordsResult = new RecordsQueryRes<>(atts);
                        recordsResult.setHasMore(queryRes.getHasMore());
                        recordsResult.setTotalCount(queryRes.getTotalCount());
                    }
                }
            }
        } else {

            if (DistinctQuery.LANGUAGE.equals(query.getLanguage())) {

                Optional<RecordsQueryDao> recordsQueryDao = getRecordsDao(sourceId, RecordsQueryDao.class);

                List<String> languages = recordsQueryDao.map(RecordsQueryDao::getSupportedLanguages)
                    .orElse(Collections.emptyList());

                if (!languages.contains(DistinctQuery.LANGUAGE)) {

                    DistinctQuery distinctQuery = query.getQuery(DistinctQuery.class);
                    recordsResult = new RecordsQueryRes<>();

                    recordsResult.setRecords(getDistinctValues(sourceId,
                        distinctQuery,
                        finalQuery.getMaxItems(),
                        attributes
                    ));
                }
            } else {

                RecordsQueryDao dao = getRecordsDao(sourceId, RecordsQueryDao.class).orElse(null);

                if (dao == null) {

                    String msg = "RecordsQueryDao is not found for id = '" + sourceId + "'";
                    context.addMsg(MsgLevel.ERROR, () -> msg);

                } else {

                    recordsResult = new RecordsQueryRes<>();

                    RecordsQueryRes<?> queryRes = dao.queryRecords(query);

                    if (queryRes != null) {

                        List<RecordAtts> recAtts = getAtts(queryRes.getRecords(), attributes, rawAtts);

                        recordsResult.setRecords(recAtts);
                        recordsResult.setTotalCount(queryRes.getTotalCount());
                        recordsResult.setHasMore(queryRes.getHasMore());
                    }
                }
            }
        }

        if (recordsResult == null) {
            recordsResult = new RecordsQueryRes<>();
        }

        return RecordsUtils.metaWithDefaultApp(recordsResult, currentApp);
    }

    private List<RecordAtts> getDistinctValues(String sourceId,
                                               DistinctQuery distinctQuery,
                                               int max,
                                               Map<String, String> schema) {

        if (max == -1) {
            max = 50;
        }

        RecordsQuery recordsQuery = new RecordsQuery();
        recordsQuery.setLanguage(PredicateService.LANGUAGE_PREDICATE);
        recordsQuery.setSourceId(sourceId);
        recordsQuery.setMaxItems(Math.max(max, 20));

        Optional<Object> query = queryLangService.convertLang(distinctQuery.getQuery(),
                distinctQuery.getLanguage(),
                PredicateService.LANGUAGE_PREDICATE);

        if (!query.isPresent()) {
            log.error("Language " + distinctQuery.getLanguage() + " is not supported by Distinct Query");
            return Collections.emptyList();
        }

        Predicate predicate = Json.getMapper().convert(query.get(), Predicate.class);

        OrPredicate distinctPredicate = Predicates.or(Predicates.empty(distinctQuery.getAttribute()));
        AndPredicate fullPredicate = Predicates.and(predicate, Predicates.not(distinctPredicate));

        int found;
        int requests = 0;

        String distinctValueAlias = "_distinctValue";
        String distinctValueIdAlias = "_distinctValueId";

        Map<String, String> innerAttributes = new HashMap<>(schema);
        innerAttributes.put(distinctValueAlias, ".str");
        innerAttributes.put(distinctValueIdAlias, ".id");

        StringBuilder distinctAttSchema = new StringBuilder(distinctQuery.getAttribute() + "{");
        innerAttributes.forEach((k, v) -> distinctAttSchema.append(k).append(":").append(v).append(","));
        distinctAttSchema.setLength(distinctAttSchema.length() - 1);
        distinctAttSchema.append("}");

        Map<String, String> attributes = Collections.singletonMap("att", distinctAttSchema.toString());
        String distinctAtt = distinctQuery.getAttribute();

        HashMap<String, DataValue> values = new HashMap<>();

        do {

            recordsQuery.setQuery(fullPredicate);
            RecordsQueryRes<RecordAtts> queryResult = query(recordsQuery, attributes, true);
            if (queryResult == null) {
                queryResult = new RecordsQueryRes<>();
            }
            found = queryResult.getRecords().size();

            for (RecordAtts value : queryResult.getRecords()) {

                DataValue att = value.get("att");
                String attStr = att.get(distinctValueAlias).asText();

                if (att.isNull() || attStr.isEmpty()) {
                    recordsQuery.setSkipCount(recordsQuery.getSkipCount() + 1);
                } else {
                    DataValue replaced = values.put(attStr, att);
                    if (replaced == null) {
                        distinctPredicate.addPredicate(Predicates.eq(distinctAtt, attStr));
                    }
                }
            }

        } while (found > 0 && values.size() <= max && ++requests <= max);

        return values.values().stream().filter(DataValue::isObject).map(v -> {

            ObjectData atts = v.asObjectData();
            RecordRef ref = RecordRef.valueOf(atts.get(distinctValueIdAlias).asText());

            atts.remove(distinctValueAlias);
            atts.remove(distinctValueIdAlias);

            return new RecordAtts(ref, atts);
        }).collect(Collectors.toList());
    }

    private RecordsQuery updateQueryLanguage(RecordsQuery recordsQuery, RecordsQueryDao dao) {

        if (dao == null) {
            return null;
        }

        List<String> supportedLanguages = dao.getSupportedLanguages();

        if (supportedLanguages.isEmpty()) {
            return recordsQuery;
        }

        Optional<QueryWithLang> queryWithLangOpt = queryLangService.convertLang(recordsQuery.getQuery(),
                recordsQuery.getLanguage(),
                supportedLanguages);

        if (queryWithLangOpt.isPresent()) {
            recordsQuery = new RecordsQuery(recordsQuery);
            QueryWithLang queryWithLang = queryWithLangOpt.get();
            recordsQuery.setQuery(queryWithLang.getQuery());
            recordsQuery.setLanguage(queryWithLang.getLanguage());
            return recordsQuery;
        }

        return null;
    }

    @Nullable
    @Override
    public List<RecordAtts> getAtts(@NotNull List<?> records,
                                    @NotNull Map<String, String> attributes,
                                    boolean rawAtts) {

        RequestContext context = RequestContext.getCurrentNotNull();

        if (log.isDebugEnabled()) {
            log.debug("getMeta start.\nRecords: " + records + " attributes: " + attributes);
        }

        List<ValueWithIdx<Object>> recordObjs = new ArrayList<>();
        List<ValueWithIdx<RecordRef>> recordRefs = new ArrayList<>();

        int idx = 0;
        for (Object rec : records) {
            if (rec instanceof RecordRef) {
                RecordRef ref = (RecordRef) rec;
                if (ref.getAppName().equals(currentApp)) {
                    ref = ref.removeAppName();
                }
                recordRefs.add(new ValueWithIdx<>(ref, idx));
            } else {
                recordObjs.add(new ValueWithIdx<>(rec, idx));
            }
            idx++;
        }

        List<ValueWithIdx<RecordAtts>> result = new ArrayList<>();
        List<RecordAtts> refsAtts = getMetaImpl(recordRefs.stream()
            .map(ValueWithIdx::getValue)
            .collect(Collectors.toList()), attributes, rawAtts);

        if (refsAtts != null && refsAtts.size() == recordRefs.size()) {
            for (int i = 0; i < refsAtts.size(); i++) {
                result.add(new ValueWithIdx<>(refsAtts.get(i), recordRefs.get(i).getIdx()));
            }
        } else {
            context.addMsg(MsgLevel.ERROR, () -> "Results count doesn't match with " +
                "requested. refsAtts: " + refsAtts + " recordRefs: " + recordRefs);
        }

        List<RecordAtts> atts = recordsAttsService.getAtts(recordObjs.stream()
            .map(ValueWithIdx::getValue)
            .collect(Collectors.toList()), attributes, rawAtts);

        if (atts != null && atts.size() == recordObjs.size()) {

            for (int i = 0; i < atts.size(); i++) {
                result.add(new ValueWithIdx<>(atts.get(i), recordObjs.get(i).getIdx()));
            }

        } else {
            context.addMsg(MsgLevel.ERROR, () -> "Results count doesn't match with " +
                "requested. atts: " + atts + " recordObjs: " + recordObjs);
        }

        if (log.isDebugEnabled()) {
            log.debug("getMeta end.\nRecords: " + records + " attributes: " + attributes);
        }

        result.sort(Comparator.comparingInt(ValueWithIdx::getIdx));

        return result.stream()
            .map(ValueWithIdx::getValue)
            .collect(Collectors.toList());
    }

    private List<RecordAtts> getMetaImpl(Collection<RecordRef> records,
                                         Map<String, String> attributes,
                                         boolean rawAtts) {

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        RequestContext context = RequestContext.getCurrentNotNull();

        if (attributes.isEmpty()) {
            return records.stream()
                .map(RecordAtts::new)
                .collect(Collectors.toList());
        }

        List<ValueWithIdx<RecordAtts>> results = new ArrayList<>();

        RecordsUtils.groupRefBySource(records).forEach((sourceId, recs) -> {

            RecordsAttsDao recordsDao = getRecordsDao(sourceId, RecordsAttsDao.class).orElse(null);

            if (recordsDao == null) {

                context.addMsg(MsgLevel.ERROR, () ->
                    "RecordsAttsDao is not found with ID: '" + sourceId + "'");

                for (ValueWithIdx<RecordRef> ref : recs) {
                    results.add(ref.map(RecordAtts::new));
                }

            } else {

                List<?> recAtts = recordsDao.getRecordsAtts(recs.stream()
                    .map(r -> r.getValue().getId())
                    .collect(Collectors.toList()));

                if (recAtts == null) {

                    for (ValueWithIdx<RecordRef> ref : recs) {
                        results.add(ref.map(RecordAtts::new));
                    }

                } else {

                    if (recAtts.size() != recs.size()) {

                        context.addMsg(MsgLevel.ERROR, () ->
                            "getRecordAtts should return " +
                                "same amount of values as in argument. " +
                                "SourceId: " + sourceId + "' " +
                                "Expected length: " + recs.size() + " " +
                                "Actual length: " + recAtts.size() + " " +
                                "Refs: " + recs + " Atts: " + recAtts);

                        for (ValueWithIdx<RecordRef> ref : recs) {
                            results.add(ref.map(RecordAtts::new));
                        }

                    } else {

                        List<RecordAtts> atts = recordsAttsService.getAtts(recAtts, attributes, rawAtts);

                        for (int i = 0; i < recs.size(); i++) {
                            results.add(new ValueWithIdx<>(atts.get(i), i));
                        }
                    }
                }
            }
        });

        results.sort(Comparator.comparingInt(ValueWithIdx::getIdx));
        return results.stream().map(ValueWithIdx::getValue).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public List<RecordRef> mutate(@NotNull List<RecordAtts> records) {

        List<RecordRef> daoResult = new ArrayList<>();

        Map<RecordRef, RecordRef> refsMapping = new HashMap<>();

        records.forEach(record -> {

            String appName = record.getId().getSourceId();
            String sourceId = record.getId().getSourceId();

            if (currentApp.equals(appName)) {

                RecordRef newId = record.getId().removeAppName();
                refsMapping.put(newId, record.getId());
                record = new RecordAtts(record, newId);
            }

            RecordsMutateDao dao = needRecordsDao(sourceId, RecordsMutateDao.class);
            RecordRef localRef = RecordRef.create("", record.getId().getId());

            List<RecordRef> mutRes = dao.mutate(Collections.singletonList(new RecordAtts(record, localRef)));
            if (mutRes == null) {
                mutRes = Collections.singletonList(record.getId());
            } else {
                mutRes = mutRes.stream()
                    .map(r -> {
                        if (StringUtils.isBlank(r.getSourceId())) {
                            return RecordRef.create(sourceId, r.getId());
                        }
                        return r;
                    }).collect(Collectors.toList());
            }

            daoResult.addAll(mutRes);
        });

        List<RecordRef> result = daoResult;
        if (!refsMapping.isEmpty()) {
            result = daoResult
                .stream()
                .map(ref -> refsMapping.getOrDefault(ref, ref))
                .collect(Collectors.toList());
        }
        return result;
    }

    @NotNull
    @Override
    public List<DelStatus> delete(@NotNull List<RecordRef> records) {

        List<DelStatus> daoResult = new ArrayList<>();

        records.forEach(record -> {

            String sourceId = record.getSourceId();

            RecordsDeleteDao dao = needRecordsDao(sourceId, RecordsDeleteDao.class);

            List<DelStatus> delResult = dao.delete(Collections.singletonList(record.getId()));
            daoResult.add(delResult != null && !delResult.isEmpty() ? delResult.get(0) : DelStatus.OK);
        });

        return daoResult;
    }

    private DaoWithConvQuery getDaoWithQuery(RecordsQuery query) {

        String sourceId = query.getSourceId();
        int sourceDelimIdx = sourceId.indexOf(RecordRef.SOURCE_DELIMITER);
        String innerSourceId = "";
        if (sourceDelimIdx > 0) {
            innerSourceId = sourceId.substring(sourceDelimIdx + 1);
            sourceId = sourceId.substring(0, sourceDelimIdx);
        }

        RecordsQueryDao dao = needRecordsDao(sourceId, RecordsQueryDao.class);
        RecordsQuery convertedQuery = updateQueryLanguage(query, dao);

        if (convertedQuery == null) {
            throw new LanguageNotSupportedException(sourceId, query.getLanguage());
        }

        convertedQuery = new RecordsQuery(convertedQuery);
        convertedQuery.setSourceId(innerSourceId);

        return new DaoWithConvQuery(dao, convertedQuery);
    }

    public void register(String sourceId, RecordsDao recordsDao) {

        if (sourceId == null) {
            log.error("id is a mandatory parameter for RecordsDao."
                + " Type: " + recordsDao.getClass()
                + " toString: " + recordsDao);
            return;
        }

        allDao.put(sourceId, recordsDao);
        register(sourceId, attsDao, RecordsAttsDao.class, recordsDao);
        register(sourceId, queryDao, RecordsQueryDao.class, recordsDao);
        register(sourceId, mutateDao, RecordsMutateDao.class, recordsDao);
        register(sourceId, deleteDao, RecordsDeleteDao.class, recordsDao);

        if (recordsDao instanceof ServiceFactoryAware) {
            ((ServiceFactoryAware) recordsDao).setRecordsServiceFactory(serviceFactory);
        }

        if (recordsDao instanceof JobsProvider) {
            List<Job> jobs = ((JobsProvider) recordsDao).getJobs();
            for (Job job : jobs) {
                jobExecutor.addJob(job);
            }
        }
    }

    private <T extends RecordsDao> void register(String id, Map<String, T> map, Class<T> type, RecordsDao value) {
        if (type.isAssignableFrom(value.getClass())) {
            @SuppressWarnings("unchecked")
            T dao = (T) value;
            map.put(id, dao);
        }
    }

    @Nullable
    @Override
    public RecsSourceInfo getSourceInfo(@NotNull String sourceId) {

        RecordsDao recordsDao = allDao.get(sourceId);
        if (recordsDao == null) {
            return null;
        }

        RecsSourceInfo recordsSourceInfo = new RecsSourceInfo();
        recordsSourceInfo.setId(sourceId);

        if (recordsDao instanceof RecordsQueryDao) {
            RecordsQueryDao queryDao = (RecordsQueryDao) recordsDao;
            recordsSourceInfo.setSupportedLanguages(queryDao.getSupportedLanguages());
            recordsSourceInfo.setFeature(RecsDaoFeature.QUERY, true);
        } else {
            recordsSourceInfo.setFeature(RecsDaoFeature.QUERY, false);
        }

        recordsSourceInfo.setFeature(RecsDaoFeature.MUTATE, recordsDao instanceof RecordsMutateDao);
        recordsSourceInfo.setFeature(RecsDaoFeature.DELETE, recordsDao instanceof RecordsDeleteDao);
        recordsSourceInfo.setFeature(RecsDaoFeature.GET_ATTS, recordsDao instanceof RecordsAttsDao);

        ColumnsSourceId columnsSourceId = recordsDao.getClass().getAnnotation(ColumnsSourceId.class);
        if (columnsSourceId != null && StringUtils.isNotBlank(columnsSourceId.value())) {
            recordsSourceInfo.setColumnsSourceId(columnsSourceId.value());
        }

        return recordsSourceInfo;
    }

    @NotNull
    @Override
    public List<RecsSourceInfo> getSourceInfo() {
        return allDao.keySet()
            .stream()
            .map(this::getSourceInfo)
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends RecordsDao> Optional<T> getRecordsDao(String sourceId, Class<T> type) {
        if (sourceId == null) {
            sourceId = "";
        }
        Map<String, ? extends RecordsDao> daoMap = daoMapByType.get(type);
        if (daoMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) daoMap.get(sourceId));
    }

    private <T extends RecordsDao> T needRecordsDao(String sourceId, Class<T> type) {
        Optional<T> source = getRecordsDao(sourceId, type);
        if (!source.isPresent()) {
            throw new RecsSourceNotFoundException(sourceId, type);
        }
        return source.get();
    }

    public boolean containsDao(String id) {
        return allDao.containsKey(id);
    }

    @Data
    @AllArgsConstructor
    private static class QueryResult {
        @NotNull
        private RecordsQueryRes<?> result;
        @Nullable
        private RecordsDao recordsDao;
    }

    private static class DaoWithConvQuery {

        final RecordsQueryDao dao;
        final RecordsQuery query;

        public DaoWithConvQuery(RecordsQueryDao dao, RecordsQuery query) {
            this.dao = dao;
            this.query = query;
        }
    }
}
