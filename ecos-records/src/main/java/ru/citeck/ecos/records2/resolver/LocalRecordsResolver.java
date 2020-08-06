package ru.citeck.ecos.records2.resolver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records2.exception.LanguageNotSupportedException;
import ru.citeck.ecos.records2.exception.RecordsException;
import ru.citeck.ecos.records2.exception.RecordsSourceNotFoundException;
import ru.citeck.ecos.records2.meta.AttributesSchema;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.AndPredicate;
import ru.citeck.ecos.records2.predicate.model.OrPredicate;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.querylang.QueryLangService;
import ru.citeck.ecos.records2.querylang.QueryWithLang;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.error.RecordsError;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.info.ColumnsSourceId;
import ru.citeck.ecos.records2.source.common.group.RecordsGroupDao;
import ru.citeck.ecos.records2.source.dao.*;
import ru.citeck.ecos.records2.source.info.RecordsSourceInfo;
import ru.citeck.ecos.records2.source.dao.local.job.Job;
import ru.citeck.ecos.records2.source.dao.local.job.JobExecutor;
import ru.citeck.ecos.records2.source.dao.local.job.JobsProvider;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class LocalRecordsResolver implements RecordsResolver, RecordsDaoRegistry {

    private static final String DEBUG_QUERY_TIME = "queryTimeMs";
    private static final String DEBUG_META_SCHEMA = "schema";

    private final Map<String, RecordsDao> allDao = new ConcurrentHashMap<>();
    private final Map<String, RecordsMetaDao> metaDao = new ConcurrentHashMap<>();
    private final Map<String, RecordsQueryDao> queryDao = new ConcurrentHashMap<>();
    private final Map<String, MutableRecordsDao> mutableDao = new ConcurrentHashMap<>();

    private final Map<Class<? extends RecordsDao>, Map<String, ? extends RecordsDao>> daoMapByType;

    private final QueryLangService queryLangService;
    private final RecordsServiceFactory serviceFactory;
    private final RecordsMetaService recordsMetaService;

    private final String currentApp;

    private final JobExecutor jobExecutor = new JobExecutor();

    public LocalRecordsResolver(RecordsServiceFactory serviceFactory) {

        this.serviceFactory = serviceFactory;
        this.recordsMetaService = serviceFactory.getRecordsMetaService();
        this.queryLangService = serviceFactory.getQueryLangService();
        this.currentApp = serviceFactory.getProperties().getAppName();

        daoMapByType = new HashMap<>();
        daoMapByType.put(RecordsMetaDao.class, metaDao);
        daoMapByType.put(RecordsQueryDao.class, queryDao);
        daoMapByType.put(MutableRecordsDao.class, mutableDao);
    }

    public void initJobs(ScheduledExecutorService executor) {
        this.jobExecutor.init(executor);
    }

    @NotNull
    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(@NotNull RecordsQuery query,
                                                       @NotNull Map<String, String> attributes,
                                                       boolean flat) {

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

        RecordsQueryResult<RecordMeta> recordsResult = null;

        if (!query.getGroupBy().isEmpty()) {

            RecordsQueryDao dao = getRecordsDao(sourceId, RecordsQueryDao.class).orElse(null);

            if (dao == null || !dao.isGroupingSupported()) {

                RecordsQueryDao groupsSource = needRecordsDao(RecordsGroupDao.ID, RecordsQueryDao.class);

                RecordsQuery convertedQuery = updateQueryLanguage(query, groupsSource);

                if (convertedQuery == null) {
                    String errorMsg = "GroupBy is not supported by language: "
                                      + query.getLanguage() + ". Query: " + query;
                    log.warn(errorMsg);
                    recordsResult = queryWithSchema(finalQuery, attributes, flat);
                    recordsResult.addError(new RecordsError(errorMsg));
                } else {
                    recordsResult = doWithSchema(attributes, flat,
                        schema -> groupsSource.queryRecords(convertedQuery, schema));
                }
            }
        }

        if (recordsResult == null && DistinctQuery.LANGUAGE.equals(query.getLanguage())) {

            Optional<RecordsQueryDao> recordsQueryDao = getRecordsDao(sourceId, RecordsQueryDao.class);

            List<String> languages = recordsQueryDao.map(RecordsQueryDao::getSupportedLanguages)
                    .orElse(Collections.emptyList());

            if (!languages.contains(DistinctQuery.LANGUAGE)) {

                DistinctQuery distinctQuery = query.getQuery(DistinctQuery.class);
                recordsResult = new RecordsQueryResult<>();

                String finalSourceId = sourceId;
                recordsResult.setRecords(doWithSchema(attributes, flat, schema ->
                    new RecordsResult<>(getDistinctValues(finalSourceId,
                        distinctQuery,
                        finalQuery.getMaxItems(),
                        schema
                    )
                )).getRecords());

            }
        }

        if (recordsResult == null) {
            recordsResult = queryWithSchema(finalQuery, attributes, flat);
        }

        return RecordsUtils.metaWithDefaultApp(recordsResult, currentApp);
    }

    private List<RecordMeta> getDistinctValues(String sourceId,
                                               DistinctQuery distinctQuery,
                                               int max,
                                               AttributesSchema schema) {

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

        Map<String, String> innerAttributes = new HashMap<>(schema.getAttributes());
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
            RecordsQueryResult<RecordMeta> queryResult = queryRecords(recordsQuery, attributes, true);
            found = queryResult.getRecords().size();

            for (RecordMeta value : queryResult.getRecords()) {

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

            return new RecordMeta(ref, atts);
        }).collect(Collectors.toList());
    }

    private RecordsQuery updateQueryLanguage(RecordsQuery recordsQuery, RecordsQueryDao dao) {

        if (dao == null) {
            return null;
        }

        List<String> supportedLanguages = dao.getSupportedLanguages();

        if (supportedLanguages == null || supportedLanguages.isEmpty()) {
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

    private QueryResult queryRecordsImpl(RecordsQuery query, AttributesSchema schema) {

        QueryResult records = null;
        List<RecordsException> exceptions = new ArrayList<>();

        try {
            records = queryRecordsWithMeta(query, schema);
        } catch (RecordsException e) {
            exceptions.add(e);
        }

        if (records == null) {

            log.error("Query failed. \n" + query + "\nSchema:\n" + schema);
            log.error("Exceptions: \n" + exceptions.stream()
                                                         .map(Throwable::getMessage)
                                                         .collect(Collectors.joining("\n")));
            records = new QueryResult(new RecordsQueryResult<>(), null);
            records.getResult().setErrors(exceptions.stream()
                                        .map(e -> new RecordsError(e.getMessage()))
                                        .collect(Collectors.toList()));
        }

        if (query.isDebug()) {
            records.getResult().setDebugInfo(getClass(), DEBUG_META_SCHEMA, schema);
        }

        return records;
    }

    private QueryResult queryRecordsWithMeta(RecordsQuery query, AttributesSchema schema) {

        DaoWithConvQuery daoWithQuery = getDaoWithQuery(query);

        if (log.isDebugEnabled()) {
            log.debug("Start records with meta query: " + daoWithQuery.query.getQuery() + "\n" + schema);
        }

        long queryStart = System.currentTimeMillis();
        RecordsQueryResult<RecordMeta> records = daoWithQuery.dao.queryRecords(daoWithQuery.query, schema);
        if (records == null) {
            records = new RecordsQueryResult<>();
        }
        long queryDuration = System.currentTimeMillis() - queryStart;

        if (log.isDebugEnabled()) {
            log.debug("Stop records with meta query. Duration: " + queryDuration);
        }

        if (query.isDebug()) {
            records.setDebugInfo(getClass(), DEBUG_QUERY_TIME, queryDuration);
        }

        return new QueryResult(records, daoWithQuery.dao);
    }

    @NotNull
    @Override
    public RecordsResult<RecordMeta> getMeta(@NotNull Collection<RecordRef> records,
                                             @NotNull Map<String, String> attributes,
                                             boolean flat) {

        if (log.isDebugEnabled()) {
            log.debug("getMeta start.\nRecords: " + records + " attributes: " + attributes);
        }

        Map<RecordRef, RecordRef> refsMapping = new HashMap<>();

        records = records.stream().map(ref -> {
            if (ref.getAppName().equals(currentApp)) {
                RecordRef newRef = ref.removeAppName();
                refsMapping.put(newRef, ref);
                ref = newRef;
            }
            return ref;
        }).collect(Collectors.toList());

        Collection<RecordRef> finalRecords = records;
        RecordsResult<RecordMeta> results = doWithSchema(attributes, flat, schema -> getMetaImpl(finalRecords, schema));

        if (log.isDebugEnabled()) {
            log.debug("getMeta end.\nRecords: " + records + " attributes: " + attributes);
        }

        results.setRecords(results.getRecords()
            .stream()
            .map(meta -> {
                RecordRef ref = meta.getId();
                return meta.withId(refsMapping.getOrDefault(ref, ref));
            })
            .collect(Collectors.toList())
        );

        return results;
    }

    private RecordsResult<RecordMetaWithDao> getMetaImpl(Collection<RecordRef> records, AttributesSchema schema) {

        RecordsResult<RecordMetaWithDao> results = new RecordsResult<>();
        if (schema.getAttributes().isEmpty()) {
            results.setRecords(records.stream()
                .map(RecordMeta::new)
                .map(m -> new RecordMetaWithDao(m, null))
                .collect(Collectors.toList()));
            return results;
        }

        RecordsUtils.groupRefBySource(records).forEach((sourceId, recs) -> {

            Optional<RecordsMetaDao> recordsDao = getRecordsDao(sourceId, RecordsMetaDao.class);
            RecordsResult<RecordMetaWithDao> meta;

            if (recordsDao.isPresent()) {

                meta = new RecordsResult<>(
                    recordsDao.get().getMeta(new ArrayList<>(records), schema),
                    m -> new RecordMetaWithDao(m, recordsDao.get())
                );

            } else {

                meta = new RecordsResult<>();
                meta.setRecords(recs.stream()
                    .map(RecordMeta::new)
                    .map(m -> new RecordMetaWithDao(m, null))
                    .collect(Collectors.toList()));
                meta.addError(new RecordsError("Records source '" + sourceId + "' can't return attributes"));

                log.debug("Records source '" + sourceId + "' can't return attributes");
            }

            results.merge(meta);
        });

        return results;
    }

    private <T extends RecordsResult<?>,
             R extends RecordsResult<RecordMeta>> R queryWithSchema(RecordsQuery query,
                                                                    Map<String, String> attributes,
                                                                    boolean flat) {
        return doWithSchema(attributes, flat, schema -> {

            QueryResult queryResult = queryRecordsImpl(query, schema);
            if (queryResult.getRecordsDao() != null) {
                return new RecordsQueryResult<>(queryResult.getResult(),
                    m -> new RecordMetaWithDao(m, queryResult.getRecordsDao()));
            }
            return queryResult.getResult();
        });
    }

    private <T extends RecordsResult<?>,
             R extends RecordsResult<RecordMeta>> R doWithSchema(Map<String, String> attributes,
                                                                 boolean flat,
                                                                 Function<AttributesSchema, T> action) {

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        T metaRes = action.apply(schema);
        List<RecordMeta> metaList = new ArrayList<>();

        for (Object record : metaRes.getRecords()) {

            boolean flattingRequired;
            RecordMeta meta;

            if (record instanceof RecordMetaWithDao) {

                RecordMetaWithDao metaWithDao = (RecordMetaWithDao) record;
                flattingRequired = flat
                    && metaWithDao.getDao() != null
                    && metaWithDao.getDao().isRawAttributesProvided();
                meta = metaWithDao.getMeta();

            } else if (record instanceof RecordMeta) {

                flattingRequired = flat;
                meta = (RecordMeta) record;

            } else {

                log.error("Unknown record type will be skipped: " + record);

                continue;
            }

            metaList.add(recordsMetaService.convertMetaResult(meta, schema, flattingRequired));
        }

        @SuppressWarnings("unchecked")
        R result = (R) metaRes;
        result.setRecords(metaList);

        return result;
    }

    @NotNull
    @Override
    public RecordsMutResult mutate(@NotNull RecordsMutation mutation) {

        RecordsMutResult result = new RecordsMutResult();

        Map<RecordRef, RecordRef> refsMapping = new HashMap<>();

        mutation.getRecords().forEach(record -> {

            if (currentApp.equals(record.getId().getAppName())) {

                RecordRef newId = record.getId().removeAppName();
                refsMapping.put(newId, record.getId());
                record = new RecordMeta(record, newId);
            }

            MutableRecordsDao dao = needRecordsDao(record.getId().getSourceId(), MutableRecordsDao.class);
            RecordsMutation sourceMut = new RecordsMutation();
            sourceMut.setRecords(Collections.singletonList(record));
            sourceMut.setDebug(mutation.isDebug());
            result.merge(dao.mutate(sourceMut));
        });

        if (!refsMapping.isEmpty()) {
            result.setRecords(result.getRecords()
                .stream()
                .map(meta -> {
                    RecordRef ref = meta.getId();
                    return meta.withId(refsMapping.getOrDefault(ref, ref));
                })
                .collect(Collectors.toList())
            );
        }
        return result;
    }

    @NotNull
    @Override
    public RecordsDelResult delete(@NotNull RecordsDeletion deletion) {

        RecordsDelResult result = new RecordsDelResult();

        Map<RecordRef, RecordRef> refsMapping = new HashMap<>();

        RecordsUtils.groupRefBySource(deletion.getRecords()).forEach((sourceId, sourceRecords) -> {

            MutableRecordsDao source = needRecordsDao(sourceId, MutableRecordsDao.class);

            RecordsDeletion sourceDeletion = new RecordsDeletion();
            sourceDeletion.setRecords(sourceRecords.stream().map(ref -> {
                if (ref.getAppName().equals(currentApp)) {
                    RecordRef newRef = ref.removeAppName();
                    refsMapping.put(newRef, ref);
                    ref = newRef;
                }
                return ref;
            }).collect(Collectors.toList()));

            result.merge(source.delete(sourceDeletion));
        });

        if (!refsMapping.isEmpty()) {
            result.setRecords(result.getRecords()
                .stream()
                .map(meta -> {
                    RecordRef ref = meta.getId();
                    return meta.withId(refsMapping.getOrDefault(ref, ref));
                })
                .collect(Collectors.toList())
            );
        }

        return result;
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
        register(sourceId, metaDao, RecordsMetaDao.class, recordsDao);
        register(sourceId, queryDao, RecordsQueryDao.class, recordsDao);
        register(sourceId, mutableDao, MutableRecordsDao.class, recordsDao);

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
    public RecordsSourceInfo getSourceInfo(@NotNull String sourceId) {

        RecordsDao recordsDao = allDao.get(sourceId);
        if (recordsDao == null) {
            return null;
        }

        RecordsSourceInfo recordsSourceInfo = new RecordsSourceInfo();
        recordsSourceInfo.setId(sourceId);

        if (recordsDao instanceof RecordsQueryDao) {
            RecordsQueryDao queryDao = (RecordsQueryDao) recordsDao;
            List<String> languages = queryDao.getSupportedLanguages();
            recordsSourceInfo.setSupportedLanguages(languages != null ? languages : Collections.emptyList());
            recordsSourceInfo.setQuerySupported(true);
            recordsSourceInfo.setQueryWithMetaSupported(true);
        }
        if (recordsDao instanceof MutableRecordsDao) {
            recordsSourceInfo.setMutationSupported(true);
        }
        if (recordsDao instanceof RecordsMetaDao) {
            recordsSourceInfo.setMetaSupported(true);
        }

        ColumnsSourceId columnsSourceId = recordsDao.getClass().getAnnotation(ColumnsSourceId.class);
        if (columnsSourceId != null && StringUtils.isNotBlank(columnsSourceId.value())) {
            recordsSourceInfo.setColumnsSourceId(columnsSourceId.value());
        }

        return recordsSourceInfo;
    }

    @NotNull
    @Override
    public List<RecordsSourceInfo> getSourceInfo() {
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
            throw new RecordsSourceNotFoundException(sourceId, type);
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
        private RecordsQueryResult<RecordMeta> result;
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

    @Data
    @AllArgsConstructor
    private static class RecordMetaWithDao {
        @NotNull
        private RecordMeta meta;
        @Nullable
        private RecordsDao dao;
    }
}
