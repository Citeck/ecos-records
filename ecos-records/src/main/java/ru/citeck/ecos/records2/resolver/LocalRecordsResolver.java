package ru.citeck.ecos.records2.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.model.AndPredicate;
import ru.citeck.ecos.predicate.model.OrPredicate;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.Predicates;
import ru.citeck.ecos.querylang.QueryLangService;
import ru.citeck.ecos.querylang.QueryWithLang;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records2.exception.LanguageNotSupportedException;
import ru.citeck.ecos.records2.exception.RecordsException;
import ru.citeck.ecos.records2.exception.RecordsSourceNotFoundException;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.error.RecordsError;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.common.group.RecordsGroupDAO;
import ru.citeck.ecos.records2.source.dao.*;
import ru.citeck.ecos.records2.utils.RecordsUtils;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class LocalRecordsResolver implements RecordsResolver, RecordsDAORegistry {

    private static final String DEBUG_QUERY_TIME = "queryTimeMs";
    private static final String DEBUG_META_SCHEMA = "schema";

    private Map<String, RecordsMetaDAO> metaDAO = new ConcurrentHashMap<>();
    private Map<String, RecordsQueryDAO> queryDAO = new ConcurrentHashMap<>();
    private Map<String, MutableRecordsDAO> mutableDAO = new ConcurrentHashMap<>();
    private Map<String, RecordsQueryWithMetaDAO> queryWithMetaDAO = new ConcurrentHashMap<>();

    private Map<Class<? extends RecordsDAO>, Map<String, ? extends RecordsDAO>> daoMapByType;

    private PredicateService predicateService;
    private QueryLangService queryLangService;

    private RecordsServiceFactory serviceFactory;

    private String currentApp;

    public LocalRecordsResolver(RecordsServiceFactory serviceFactory) {

        this.serviceFactory = serviceFactory;
        this.predicateService = serviceFactory.getPredicateService();
        this.queryLangService = serviceFactory.getQueryLangService();
        this.currentApp = serviceFactory.getProperties().getAppName();

        daoMapByType = new HashMap<>();
        daoMapByType.put(RecordsMetaDAO.class, metaDAO);
        daoMapByType.put(RecordsQueryDAO.class, queryDAO);
        daoMapByType.put(MutableRecordsDAO.class, mutableDAO);
        daoMapByType.put(RecordsQueryWithMetaDAO.class, queryWithMetaDAO);
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {

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

        RecordsQueryResult<RecordMeta> recordsResult = null;

        if (!query.getGroupBy().isEmpty()) {

            RecordsQueryBaseDAO dao = getRecordsDAO(sourceId,
                                                    RecordsQueryWithMetaDAO.class).orElse(null);

            if (dao == null || !dao.isGroupingSupported()) {

                RecordsQueryWithMetaDAO groupsSource = needRecordsDAO(RecordsGroupDAO.ID,
                                                                      RecordsQueryWithMetaDAO.class);

                RecordsQuery convertedQuery = updateQueryLanguage(query, groupsSource);

                if (convertedQuery == null) {
                    String errorMsg = "GroupBy is not supported by language: "
                                      + query.getLanguage() + ". Query: " + query;
                    log.warn(errorMsg);
                    recordsResult = queryRecordsImpl(query, schema);
                    recordsResult.addError(new RecordsError(errorMsg));
                } else {
                    recordsResult = groupsSource.queryRecords(convertedQuery, schema);
                }
            }
        }

        if (recordsResult == null && DistinctQuery.LANGUAGE.equals(query.getLanguage())) {

            Optional<RecordsQueryWithMetaDAO> recordsDAO = getRecordsDAO(sourceId, RecordsQueryWithMetaDAO.class);
            Optional<RecordsQueryDAO> recordsQueryDAO = getRecordsDAO(sourceId, RecordsQueryDAO.class);

            List<String> languages = recordsDAO.map(RecordsQueryBaseDAO::getSupportedLanguages)
                    .orElse(recordsQueryDAO.map(RecordsQueryBaseDAO::getSupportedLanguages)
                    .orElse(Collections.emptyList()));

            if (!languages.contains(DistinctQuery.LANGUAGE)) {

                DistinctQuery distinctQuery = query.getQuery(DistinctQuery.class);
                recordsResult = new RecordsQueryResult<>();

                recordsResult.setRecords(getDistinctValues(sourceId,
                                                           distinctQuery,
                                                           query.getMaxItems(),
                                                           schema));
            }
        }

        if (recordsResult == null) {
            recordsResult = queryRecordsImpl(query, schema);
        }

        return RecordsUtils.metaWithDefaultApp(recordsResult, currentApp);
    }

    private List<RecordMeta> getDistinctValues(String sourceId, DistinctQuery distinctQuery, int max, String schema) {

        if (max == -1) {
            max = 50;
        }

        RecordsQuery recordsQuery = new RecordsQuery();
        recordsQuery.setLanguage(PredicateService.LANGUAGE_PREDICATE);
        recordsQuery.setSourceId(sourceId);
        recordsQuery.setMaxItems(Math.max(max, 20));

        Optional<JsonNode> query = queryLangService.convertLang(distinctQuery.getQuery(),
                distinctQuery.getLanguage(),
                PredicateService.LANGUAGE_PREDICATE);

        if (!query.isPresent()) {
            log.error("Language " + distinctQuery.getLanguage() + " is not supported by Distinct Query");
            return Collections.emptyList();
        }

        Predicate predicate = predicateService.readJson(query.get());

        OrPredicate distinctPredicate = Predicates.or(Predicates.empty(distinctQuery.getAttribute()));
        AndPredicate fullPredicate = Predicates.and(predicate, Predicates.not(distinctPredicate));

        int found;
        int requests = 0;

        String distinctValueAlias = "_distinctValue";
        String attSchema = "att(n:\"" + distinctQuery.getAttribute() + "\")"
                                + "{id," + distinctValueAlias + ":str, " + schema + "}";

        String distinctAtt = distinctQuery.getAttribute();

        HashMap<String, JsonNode> values = new HashMap<>();

        do {

            recordsQuery.setQuery(predicateService.writeJson(fullPredicate));
            RecordsQueryResult<RecordMeta> queryResult = queryRecords(recordsQuery, attSchema);
            found = queryResult.getRecords().size();

            for (RecordMeta value : queryResult.getRecords()) {

                JsonNode att = value.get("att");
                String strVal = att.path(distinctValueAlias).asText();

                if (att.isMissingNode() || att.isNull()) {
                    recordsQuery.setSkipCount(recordsQuery.getSkipCount() + 1);
                } else {
                    JsonNode replaced = values.put(strVal, att);
                    if (replaced == null) {
                        distinctPredicate.addPredicate(Predicates.eq(distinctAtt, strVal));
                    }
                }
            }

        } while (found > 0 && values.size() <= max && ++requests <= max);

        return values.values().stream().filter(JsonNode::isObject).map(v -> {
            ObjectNode attributes = (ObjectNode) v;
            RecordRef ref = RecordRef.valueOf(attributes.path("id").asText());
            attributes.remove(distinctValueAlias);
            attributes.remove("id");
            return new RecordMeta(ref, attributes);
        }).collect(Collectors.toList());
    }

    private RecordsQuery updateQueryLanguage(RecordsQuery recordsQuery, RecordsQueryBaseDAO dao) {

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

    private RecordsQueryResult<RecordMeta> queryRecordsImpl(RecordsQuery query, String schema) {

        RecordsQueryResult<RecordMeta> records = null;
        List<RecordsException> exceptions = new ArrayList<>();

        boolean withMetaWasSkipped = true;
        if (StringUtils.isNotBlank(schema)) {
            try {
                records = queryRecordsWithMeta(query, schema);
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
                records.merge(getMeta(recordRefs.getRecords(), schema));

            } catch (RecordsException e) {
                exceptions.add(e);
            }
        }

        if (records == null && withMetaWasSkipped) {
            try {
                records = queryRecordsWithMeta(query, schema);
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

    private RecordsQueryResult<RecordMeta> queryRecordsWithMeta(RecordsQuery query, String schema) {

        DaoWithConvQuery<RecordsQueryWithMetaDAO> daoWithQuery = getDaoWithQuery(query,
                                                                                 RecordsQueryWithMetaDAO.class);

        if (log.isDebugEnabled()) {
            log.debug("Start records with meta query: " + daoWithQuery.query.getQuery() + "\n" + schema);
        }

        long queryStart = System.currentTimeMillis();
        RecordsQueryResult<RecordMeta> records = daoWithQuery.dao.queryRecords(daoWithQuery.query, schema);
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

        DaoWithConvQuery<RecordsQueryDAO> daoWithQuery = getDaoWithQuery(query, RecordsQueryDAO.class);

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

    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {

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

        RecordsResult<RecordMeta> results = getMetaImpl(records, schema);

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

    private RecordsResult<RecordMeta> getMetaImpl(Collection<RecordRef> records, String schema) {

        RecordsResult<RecordMeta> results = new RecordsResult<>();
        if (StringUtils.isBlank(schema)) {
            results.setRecords(records.stream().map(RecordMeta::new).collect(Collectors.toList()));
            return results;
        }

        RecordsUtils.groupRefBySource(records).forEach((sourceId, recs) -> {

            Optional<RecordsMetaDAO> recordsDAO = getRecordsDAO(sourceId, RecordsMetaDAO.class);
            RecordsResult<RecordMeta> meta;

            if (recordsDAO.isPresent()) {

                meta = recordsDAO.get().getMeta(new ArrayList<>(records), schema);

            } else {

                meta = new RecordsResult<>();
                meta.setRecords(recs.stream().map(RecordMeta::new).collect(Collectors.toList()));
                meta.addError(new RecordsError("Records source '" + sourceId + "' can't return attributes"));

                log.debug("Records source '" + sourceId + "' can't return attributes");
            }

            results.merge(meta);
        });

        return results;
    }

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        RecordsMutResult result = new RecordsMutResult();

        Map<RecordRef, RecordRef> refsMapping = new HashMap<>();

        mutation.getRecords().forEach(record -> {

            if (currentApp.equals(record.getId().getAppName())) {

                RecordRef newId = record.getId().removeAppName();
                refsMapping.put(newId, record.getId());
                record = new RecordMeta(record, newId);
            }

            MutableRecordsDAO dao = needRecordsDAO(record.getId().getSourceId(), MutableRecordsDAO.class);
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

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {

        RecordsDelResult result = new RecordsDelResult();

        Map<RecordRef, RecordRef> refsMapping = new HashMap<>();

        RecordsUtils.groupRefBySource(deletion.getRecords()).forEach((sourceId, sourceRecords) -> {

            MutableRecordsDAO source = needRecordsDAO(sourceId, MutableRecordsDAO.class);

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

    private <T extends RecordsQueryBaseDAO> DaoWithConvQuery<T> getDaoWithQuery(RecordsQuery query, Class<T> daoType) {

        String sourceId = query.getSourceId();
        int sourceDelimIdx = sourceId.indexOf(RecordRef.SOURCE_DELIMITER);
        String innerSourceId = "";
        if (sourceDelimIdx > 0) {
            innerSourceId = sourceId.substring(sourceDelimIdx + 1);
            sourceId = sourceId.substring(0, sourceDelimIdx);
        }

        T dao = needRecordsDAO(sourceId, daoType);
        RecordsQuery convertedQuery = updateQueryLanguage(query, dao);

        if (convertedQuery == null) {
            throw new LanguageNotSupportedException(sourceId, query.getLanguage());
        }

        convertedQuery = new RecordsQuery(convertedQuery);
        convertedQuery.setSourceId(innerSourceId);

        return new DaoWithConvQuery<>(dao, convertedQuery);
    }

    public void register(RecordsDAO recordsDao) {

        String id = recordsDao.getId();
        if (id == null) {
            throw new IllegalArgumentException("id is a mandatory parameter for RecordsDAO");
        }

        register(metaDAO, RecordsMetaDAO.class, recordsDao);
        register(queryDAO, RecordsQueryDAO.class, recordsDao);
        register(mutableDAO, MutableRecordsDAO.class, recordsDao);
        register(queryWithMetaDAO, RecordsQueryWithMetaDAO.class, recordsDao);

        if (recordsDao instanceof ServiceFactoryAware) {
            ((ServiceFactoryAware) recordsDao).setRecordsServiceFactory(serviceFactory);
        }
    }

    private <T extends RecordsDAO> void register(Map<String, T> map, Class<T> type, RecordsDAO value) {
        if (type.isAssignableFrom(value.getClass())) {
            @SuppressWarnings("unchecked")
            T dao = (T) value;
            map.put(value.getId(), dao);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends RecordsDAO> Optional<T> getRecordsDAO(String sourceId, Class<T> type) {
        if (sourceId == null) {
            sourceId = "";
        }
        Map<String, ? extends RecordsDAO> daoMap = daoMapByType.get(type);
        if (daoMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) daoMap.get(sourceId));
    }

    private <T extends RecordsDAO> T needRecordsDAO(String sourceId, Class<T> type) {
        Optional<T> source = getRecordsDAO(sourceId, type);
        if (!source.isPresent()) {
            throw new RecordsSourceNotFoundException(sourceId, type);
        }
        return source.get();
    }

    private static class DaoWithConvQuery<T extends RecordsQueryBaseDAO> {

        final T dao;
        final RecordsQuery query;

        public DaoWithConvQuery(T dao, RecordsQuery query) {
            this.dao = dao;
            this.query = query;
        }
    }
}
