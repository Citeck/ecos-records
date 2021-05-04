package ru.citeck.ecos.records2.resolver;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.source.dao.local.job.JobExecutor;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records2.exception.LanguageNotSupportedException;
import ru.citeck.ecos.records2.exception.RecordsException;
import ru.citeck.ecos.records2.exception.RecsSourceNotFoundException;
import ru.citeck.ecos.records2.querylang.QueryLangService;
import ru.citeck.ecos.records2.querylang.QueryWithLang;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.error.RecordsError;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.*;
import ru.citeck.ecos.records2.source.dao.local.job.JobsProvider;
import ru.citeck.ecos.records2.source.info.ColumnsSourceId;
import ru.citeck.ecos.records2.utils.RecordsUtils;
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @deprecated -> records3.*.LocalRecordsResolver
 */
@Slf4j
@Deprecated
public class LocalRecordsResolverV0 {

    private static final String DEBUG_QUERY_TIME = "queryTimeMs";
    private static final String DEBUG_META_SCHEMA = "schema";

    private final Map<String, RecordsDao> allDao = new ConcurrentHashMap<>();
    private final Map<String, RecordsMetaDao> metaDao = new ConcurrentHashMap<>();
    private final Map<String, RecordsQueryDao> queryDao = new ConcurrentHashMap<>();
    private final Map<String, MutableRecordsDao> mutableDao = new ConcurrentHashMap<>();
    private final Map<String, RecordsQueryWithMetaDao> queryWithMetaDao = new ConcurrentHashMap<>();

    private final Map<Class<? extends RecordsDao>, Map<String, ? extends RecordsDao>> daoMapByType;

    private final QueryLangService queryLangService;
    private final RecordsServiceFactory serviceFactory;
    private final String currentApp;
    private final JobExecutor jobExecutor;

    public LocalRecordsResolverV0(RecordsServiceFactory serviceFactory) {

        this.serviceFactory = serviceFactory;
        this.queryLangService = serviceFactory.getQueryLangService();
        this.currentApp = serviceFactory.getProperties().getAppName();
        this.jobExecutor = serviceFactory.getJobExecutor();

        daoMapByType = new HashMap<>();
        daoMapByType.put(RecordsMetaDao.class, metaDao);
        daoMapByType.put(RecordsQueryDao.class, queryDao);
        daoMapByType.put(MutableRecordsDao.class, mutableDao);
        daoMapByType.put(RecordsQueryWithMetaDao.class, queryWithMetaDao);
    }

    public RecordsQueryResult<RecordAtts> queryRecords(RecordsQuery query,
                                                       List<SchemaAtt> schema,
                                                       boolean rawAtts) {

        String sourceId = query.getSourceId();
        int appDelimIdx = sourceId.indexOf('/');

        if (appDelimIdx != -1) {

            String appName = sourceId.substring(0, appDelimIdx);

            //if appName is not current app then we in force local mode and sourceId with slash is correct
            if (appName.equals(currentApp)) {
                sourceId = sourceId.substring(appDelimIdx + 1);
                query = new RecordsQuery(query);
                query.setSourceId(sourceId);
            }
        }

        RecordsQueryResult<RecordAtts> recordsResult = queryRecordsImpl(query, schema, rawAtts);
        return RecordsUtils.attsWithDefaultApp(recordsResult, currentApp);
    }

    private RecordsQuery updateQueryLanguage(RecordsQuery recordsQuery, RecordsQueryBaseDao dao) {

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

    private RecordsQueryResult<RecordAtts> queryRecordsImpl(RecordsQuery query,
                                                            List<SchemaAtt> schema,
                                                            boolean rawAtts) {

        RecordsQueryResult<RecordAtts> records = null;
        List<RecordsException> exceptions = new ArrayList<>();

        boolean withMetaWasSkipped = true;
        if (schema.size() > 0) {
            try {
                records = queryRecordsWithMeta(query, schema, rawAtts);
            } catch (RecordsException e) {
                exceptions.add(e);
            }
            withMetaWasSkipped = false;
        }

        if (records == null) {

            RecordsQueryResult<RecordRef> recordRefs;
            try {
                recordRefs = queryRecordsWithoutMeta(query);

                records = new RecordsQueryResult<>();
                records.merge(recordRefs);
                records.setTotalCount(recordRefs.getTotalCount());
                records.merge(getMeta(recordRefs.getRecords(), schema, rawAtts));

            } catch (RecordsException e) {
                exceptions.add(e);
            }
        }

        if (records == null && withMetaWasSkipped) {
            try {
                records = queryRecordsWithMeta(query, schema, rawAtts);
            } catch (RecordsException e) {
                exceptions.add(e);
            }
        }

        if (records == null) {

            log.error("Query failed. \n" + query + "\nSchema:\n" + schema);
            log.error("Exceptions: \n" + exceptions.stream()
                                                         .map(Throwable::getMessage)
                                                         .collect(Collectors.joining("\n")));
            records = new RecordsQueryResult<>();
            records.setErrors(exceptions.stream()
                                        .map(e -> new RecordsError(e.getMessage()))
                                        .collect(Collectors.toList()));
        }

        if (query.isDebug()) {
            records.setDebugInfo(getClass(), DEBUG_META_SCHEMA, schema);
        }

        return records;
    }

    private RecordsQueryResult<RecordAtts> queryRecordsWithMeta(RecordsQuery query,
                                                                List<SchemaAtt> schema,
                                                                boolean rawAtts) {

        DaoWithConvQuery<RecordsQueryWithMetaDao> daoWithQuery = getDaoWithQuery(query,
                                                                                 RecordsQueryWithMetaDao.class);

        if (log.isDebugEnabled()) {
            log.debug("Start records with meta query: " + daoWithQuery.query.getQuery() + "\n" + schema);
        }

        long queryStart = System.currentTimeMillis();
        RecordsQueryResult<RecordAtts> records = daoWithQuery.dao.queryRecords(daoWithQuery.query, schema, rawAtts);
        long queryDuration = System.currentTimeMillis() - queryStart;

        if (log.isDebugEnabled()) {
            log.debug("Stop records with meta query. Duration: " + queryDuration);
        }

        if (query.isDebug()) {
            records.setDebugInfo(getClass(), DEBUG_QUERY_TIME, queryDuration);
        }

        return records;
    }

    private RecordsQueryResult<RecordRef> queryRecordsWithoutMeta(RecordsQuery query) {

        DaoWithConvQuery<RecordsQueryDao> daoWithQuery = getDaoWithQuery(query, RecordsQueryDao.class);

        if (log.isDebugEnabled()) {
            log.debug("Start records query: " + daoWithQuery.query.getQuery());
        }

        long recordsQueryStart = System.currentTimeMillis();
        RecordsQueryResult<RecordRef> recordRefs = daoWithQuery.dao.queryRecords(daoWithQuery.query);
        long recordsTime = System.currentTimeMillis() - recordsQueryStart;

        if (log.isDebugEnabled()) {
            int found = recordRefs.getRecords().size();
            log.debug("Stop records query. Found: " + found + " Duration: " + recordsTime);
        }
        return recordRefs;
    }

    @NotNull
    public RecordsResult<RecordAtts> getMeta(@NotNull Collection<RecordRef> records,
                                             List<SchemaAtt> schema,
                                             boolean rawAtts) {

        if (log.isDebugEnabled()) {
            log.debug("getMeta start.\nRecords: " + records + " schema: " + schema);
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

        RecordsResult<RecordAtts> results = getMetaImpl(records, schema, rawAtts);

        if (log.isDebugEnabled()) {
            log.debug("getMeta end.\nRecords: " + records + " schema: " + schema);
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

    private RecordsResult<RecordAtts> getMetaImpl(Collection<RecordRef> records,
                                                  List<SchemaAtt> schema,
                                                  boolean rawAtts) {

        RecordsResult<RecordAtts> results = new RecordsResult<>();
        if (schema.isEmpty()) {
            results.setRecords(records.stream().map(RecordAtts::new).collect(Collectors.toList()));
            return results;
        }

        RecordsUtils.groupRefBySource(records).forEach((sourceId, recs) -> {

            Optional<RecordsMetaDao> recordsDao = getRecordsDao(sourceId, RecordsMetaDao.class);
            RecordsResult<RecordAtts> meta;

            if (recordsDao.isPresent()) {

                meta = recordsDao.get().getMeta(new ArrayList<>(records), schema, rawAtts);

            } else {

                meta = new RecordsResult<>();
                meta.setRecords(recs.stream().map(r -> new RecordMeta(r.getValue())).collect(Collectors.toList()));

                log.debug("Records source '" + sourceId + "' can't return attributes");
            }

            results.merge(meta);
        });

        return results;
    }

    @NotNull
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
    public RecordsDelResult delete(@NotNull RecordsDeletion deletion) {

        RecordsDelResult result = new RecordsDelResult();

        Map<RecordRef, RecordRef> refsMapping = new HashMap<>();

        RecordsUtils.groupRefBySource(deletion.getRecords()).forEach((sourceId, sourceRecords) -> {

            MutableRecordsDao source = needRecordsDao(sourceId, MutableRecordsDao.class);

            RecordsDeletion sourceDeletion = new RecordsDeletion();
            sourceDeletion.setRecords(sourceRecords.stream().map(refWithIdx -> {

                RecordRef ref = refWithIdx.getValue();

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

    private <T extends RecordsQueryBaseDao> DaoWithConvQuery<T> getDaoWithQuery(RecordsQuery query, Class<T> daoType) {

        String sourceId = query.getSourceId();
        int sourceDelimIdx = sourceId.indexOf(RecordRef.SOURCE_DELIMITER);
        String innerSourceId = "";
        if (sourceDelimIdx > 0) {
            innerSourceId = sourceId.substring(sourceDelimIdx + 1);
            sourceId = sourceId.substring(0, sourceDelimIdx);
        }

        T dao = needRecordsDao(sourceId, daoType);
        RecordsQuery convertedQuery = updateQueryLanguage(query, dao);

        if (convertedQuery == null) {
            throw new LanguageNotSupportedException(sourceId, query.getLanguage());
        }

        convertedQuery = new RecordsQuery(convertedQuery);
        convertedQuery.setSourceId(innerSourceId);

        return new DaoWithConvQuery<>(dao, convertedQuery);
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
        register(sourceId, queryWithMetaDao, RecordsQueryWithMetaDao.class, recordsDao);

        if (recordsDao instanceof ServiceFactoryAware) {
            ((ServiceFactoryAware) recordsDao).setRecordsServiceFactory(serviceFactory);
        }

        if (recordsDao instanceof JobsProvider) {
            jobExecutor.addJobs(sourceId, ((JobsProvider) recordsDao).getJobs());
        }
    }

    public void unregister(String sourceId) {

        allDao.remove(sourceId);
        metaDao.remove(sourceId);
        queryDao.remove(sourceId);
        mutableDao.remove(sourceId);
        queryWithMetaDao.remove(sourceId);

        jobExecutor.removeJobs(sourceId);
    }

    private <T extends RecordsDao> void register(String id, Map<String, T> map, Class<T> type, RecordsDao value) {
        if (type.isAssignableFrom(value.getClass())) {
            @SuppressWarnings("unchecked")
            T dao = (T) value;
            map.put(id, dao);
        }
    }

    @Nullable
    public RecordsDaoInfo getSourceInfo(@NotNull String sourceId) {

        RecordsDao recordsDao = allDao.get(sourceId);
        if (recordsDao == null) {
            return null;
        }

        RecordsDaoInfo recordsSourceInfo = new RecordsDaoInfo();
        recordsSourceInfo.setId(sourceId);

        if (recordsDao instanceof RecordsQueryBaseDao) {
            RecordsQueryBaseDao queryDao = (RecordsQueryBaseDao) recordsDao;
            List<String> languages = queryDao.getSupportedLanguages();
            recordsSourceInfo.setSupportedLanguages(languages != null ? languages : Collections.emptyList());
            recordsSourceInfo.getFeatures().setQuery(true);
        }
        if (recordsDao instanceof MutableRecordsDao) {
            recordsSourceInfo.getFeatures().setMutate(true);
        }
        if (recordsDao instanceof RecordsMetaDao) {
            recordsSourceInfo.getFeatures().setGetAtts(true);
        }

        ColumnsSourceId columnsSourceId = recordsDao.getClass().getAnnotation(ColumnsSourceId.class);
        if (columnsSourceId != null && StringUtils.isNotBlank(columnsSourceId.value())) {
            recordsSourceInfo.setColumnsSourceId(columnsSourceId.value());
        }

        return recordsSourceInfo;
    }

    @NotNull
    public List<RecordsDaoInfo> getSourceInfo() {
        return allDao.keySet()
            .stream()
            .map(this::getSourceInfo)
            .collect(Collectors.toList());
    }

    @Nullable
    public RecordsQueryBaseDao getRecordsQueryBaseDao(String sourceId) {
        RecordsQueryBaseDao baseDao = getRecordsDao(sourceId, RecordsQueryWithMetaDao.class).orElse(null);
        if (baseDao == null) {
            baseDao = getRecordsDao(sourceId, RecordsQueryDao.class).orElse(null);
        }
        return baseDao;
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

    private static class DaoWithConvQuery<T extends RecordsQueryBaseDao> {

        final T dao;
        final RecordsQuery query;

        public DaoWithConvQuery(T dao, RecordsQuery query) {
            this.dao = dao;
            this.query = query;
        }
    }
}
