package ru.citeck.ecos.records2.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.model.AndPredicate;
import ru.citeck.ecos.predicate.model.OrPredicate;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.Predicates;
import ru.citeck.ecos.querylang.QueryLangService;
import ru.citeck.ecos.querylang.QueryWithLang;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.meta.RecordsMetaServiceAware;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LocalRecordsResolver implements RecordsResolver,
                                             RecordsServiceAware,
                                             RecordsMetaServiceAware,
                                             PredicateServiceAware,
                                             RecordsDAORegistry {

    private static final String DEBUG_QUERY_TIME = "queryTimeMs";
    private static final String DEBUG_META_SCHEMA = "schema";

    private static final Log logger = LogFactory.getLog(LocalRecordsResolver.class);

    private Map<String, RecordsMetaDAO> metaDAO = new ConcurrentHashMap<>();
    private Map<String, RecordsQueryDAO> queryDAO = new ConcurrentHashMap<>();
    private Map<String, MutableRecordsDAO> mutableDAO = new ConcurrentHashMap<>();
    private Map<String, RecordsQueryWithMetaDAO> queryWithMetaDAO = new ConcurrentHashMap<>();

    private RecordsService recordsService;
    private PredicateService predicateService;
    private QueryLangService queryLangService;
    private RecordsMetaService recordsMetaService;

    public LocalRecordsResolver(QueryLangService queryLangService) {
        this.queryLangService = queryLangService;
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {

        if (!query.getGroupBy().isEmpty()) {

            RecordsQueryWithMetaDAO groupsSource = needRecordsDAO(RecordsGroupDAO.ID,
                    RecordsQueryWithMetaDAO.class,
                    queryWithMetaDAO);
            RecordsQuery convertedQuery = updateQueryLanguage(query, groupsSource);

            if (convertedQuery == null) {
                logger.warn("GroupBy is not supported by language: " + query.getLanguage() + ". Query: " + query);
                return queryRecordsImpl(query, schema);
            }
            return groupsSource.queryRecords(convertedQuery, schema);
        }

        if (DistinctQuery.LANGUAGE.equals(query.getLanguage())) {

            Optional<RecordsQueryWithMetaDAO> recordsDAO = getRecordsDAO(query.getSourceId(), queryWithMetaDAO);
            Optional<RecordsQueryDAO> recordsQueryDAO = getRecordsDAO(query.getSourceId(), queryDAO);

            List<String> languages = recordsDAO.map(RecordsQueryBaseDAO::getSupportedLanguages)
                    .orElse(recordsQueryDAO.map(RecordsQueryBaseDAO::getSupportedLanguages)
                    .orElse(Collections.emptyList()));

            if (!languages.contains(DistinctQuery.LANGUAGE)) {

                DistinctQuery distinctQuery = query.getQuery(DistinctQuery.class);
                RecordsQueryResult<RecordMeta> result = new RecordsQueryResult<>();

                List<JsonNode> values = getDistinctValues(query.getSourceId(),
                        distinctQuery,
                        query.getMaxItems(),
                        schema);
                result.setRecords(values.stream().map(v -> {
                    RecordRef ref = RecordRef.valueOf(v.path("id").asText());
                    return new RecordMeta(ref, (ObjectNode) v);
                }).collect(Collectors.toList()));

                return result;
            }
        }

        return queryRecordsImpl(query, schema);
    }

    private List<JsonNode> getDistinctValues(String sourceId, DistinctQuery distinctQuery, int max, String schema) {

        RecordsQuery recordsQuery = new RecordsQuery();
        recordsQuery.setLanguage(PredicateService.LANGUAGE_PREDICATE);
        recordsQuery.setSourceId(sourceId);
        recordsQuery.setMaxItems(max);

        Optional<JsonNode> query = queryLangService.convertLang(distinctQuery.getQuery(),
                distinctQuery.getLanguage(),
                PredicateService.LANGUAGE_PREDICATE);

        if (!query.isPresent()) {
            logger.error("Language " + distinctQuery.getLanguage() + " is not supported by Distinct Query");
            return Collections.emptyList();
        }

        Predicate predicate = predicateService.readJson(query.get());

        OrPredicate distinctPredicate = Predicates.or(Predicates.empty(distinctQuery.getAttribute()));
        AndPredicate fullPredicate = Predicates.and(predicate, Predicates.not(distinctPredicate));

        Set<JsonNode> values = new HashSet<>();

        int found;
        int requests = 0;

        String attSchema = "att(n:\"" + distinctQuery.getAttribute() + "\"){value:str, " + schema + "}";

        do {

            recordsQuery.setQuery(predicateService.writeJson(fullPredicate));
            RecordsQueryResult<RecordMeta> queryResult = queryRecords(recordsQuery, attSchema);
            found = queryResult.getRecords().size();

            for (RecordMeta value : queryResult.getRecords()) {

                distinctPredicate.addPredicate(Predicates.equal(distinctQuery.getAttribute(),
                                               value.get("att").path("value").asText()));
            }

            queryResult.getRecords().forEach(r -> values.add(r.get("att")));

        } while (found > 0 && values.size() <= max && ++requests <= max);

        return new ArrayList<>(values);
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

        boolean withMetaWasSkipped = true;
        if (StringUtils.isNotBlank(schema)) {
            records = queryRecordsWithMeta(query, schema);
            withMetaWasSkipped = false;
        }

        if (records == null) {

            RecordsQueryResult<RecordRef> recordRefs = queryRecordsWithoutMeta(query);

            if (recordRefs != null) {
                records = new RecordsQueryResult<>();
                records.merge(recordRefs);
                records.setTotalCount(recordRefs.getTotalCount());
                records.merge(getMeta(recordRefs.getRecords(), schema));
            }
        }

        if (records == null && withMetaWasSkipped) {
            records = queryRecordsWithMeta(query, schema);
        }

        if (records == null) {
            logger.error("Query failed. \n" + query + "\nSchema:\n" + schema);
            records = new RecordsQueryResult<>();
        }

        if (query.isDebug()) {
            records.setDebugInfo(getClass(), DEBUG_META_SCHEMA, schema);
        }

        return records;
    }

    private RecordsQueryResult<RecordMeta> queryRecordsWithMeta(RecordsQuery query, String schema) {

        Optional<RecordsQueryWithMetaDAO> recordsQueryMetaDAO = getRecordsDAO(query.getSourceId(), queryWithMetaDAO);

        RecordsQuery convertedQuery = updateQueryLanguage(query, recordsQueryMetaDAO.orElse(null));

        if (convertedQuery == null) {
            return null;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Start records with meta query: " + convertedQuery.getQuery() + "\n" + schema);
        }

        long queryStart = System.currentTimeMillis();
        RecordsQueryResult<RecordMeta> records = recordsQueryMetaDAO.get().queryRecords(convertedQuery, schema);
        long queryDuration = System.currentTimeMillis() - queryStart;

        if (logger.isDebugEnabled()) {
            logger.debug("Stop records with meta query. Duration: " + queryDuration);
        }

        if (query.isDebug()) {
            records.setDebugInfo(getClass(), DEBUG_QUERY_TIME, queryDuration);
        }

        return records;
    }

    private RecordsQueryResult<RecordRef> queryRecordsWithoutMeta(RecordsQuery query) {

        Optional<RecordsQueryDAO> recordsQueryDAO = getRecordsDAO(query.getSourceId(), queryDAO);

        RecordsQuery convertedQuery = updateQueryLanguage(query, recordsQueryDAO.orElse(null));

        if (convertedQuery == null) {
            return null;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Start records query: " + convertedQuery.getQuery());
        }

        long recordsQueryStart = System.currentTimeMillis();
        RecordsQueryResult<RecordRef> recordRefs = recordsQueryDAO.get().queryRecords(convertedQuery);
        long recordsTime = System.currentTimeMillis() - recordsQueryStart;

        if (logger.isDebugEnabled()) {
            int found = recordRefs.getRecords().size();
            logger.debug("Stop records query. Found: " + found + " Duration: " + recordsTime);

        }
        return recordRefs;
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {

        if (logger.isDebugEnabled()) {
            logger.debug("getMeta start.\nRecords: " + records + " schema: " + schema);
        }

        RecordsResult<RecordMeta> results = new RecordsResult<>();

        RecordsUtils.groupRefBySource(records).forEach((sourceId, recs) -> {

            Optional<RecordsMetaDAO> recordsDAO = getRecordsDAO(sourceId, metaDAO);
            RecordsResult<RecordMeta> meta;

            if (recordsDAO.isPresent()) {

                meta = recordsDAO.get().getMeta(new ArrayList<>(records), schema);

            } else {

                meta = new RecordsResult<>();
                meta.setRecords(recs.stream().map(RecordMeta::new).collect(Collectors.toList()));
                logger.debug("Records source " + sourceId + " can't return attributes");
            }

            results.merge(meta);
        });

        if (logger.isDebugEnabled()) {
            logger.debug("getMeta end.\nRecords: " + records + " schema: " + schema);
        }

        return results;
    }

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        RecordsMutResult result = new RecordsMutResult();

        mutation.getRecords().forEach(record -> {
            MutableRecordsDAO dao = needRecordsDAO(record.getId().getSourceId(), MutableRecordsDAO.class, mutableDAO);
            RecordsMutation sourceMut = new RecordsMutation();
            sourceMut.setRecords(Collections.singletonList(record));
            sourceMut.setDebug(mutation.isDebug());
            result.merge(dao.mutate(sourceMut));
        });

        return result;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        RecordsDelResult result = new RecordsDelResult();

        RecordsUtils.groupRefBySource(deletion.getRecords()).forEach((sourceId, sourceRecords) -> {
            MutableRecordsDAO source = needRecordsDAO(sourceId, MutableRecordsDAO.class, mutableDAO);
            result.merge(source.delete(deletion));
        });

        return result;
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

        execDaoSetter(recordsDao, PredicateServiceAware.class, this.predicateService);
        execDaoSetter(recordsDao, RecordsMetaServiceAware.class, this.recordsMetaService);
        execDaoSetter(recordsDao, RecordsServiceAware.class, this.recordsService);
    }

    private <T extends RecordsDAO> void register(Map<String, T> map, Class<T> type, RecordsDAO value) {
        if (type.isAssignableFrom(value.getClass())) {
            @SuppressWarnings("unchecked")
            T dao = (T) value;
            map.put(value.getId(), dao);
        }
    }

    private <T extends RecordsDAO> Optional<T> getRecordsDAO(String sourceId, Map<String, T> registry) {
        if (sourceId == null) {
            sourceId = "";
        }
        return Optional.ofNullable(registry.get(sourceId));
    }

    private <T extends RecordsDAO> T needRecordsDAO(String sourceId, Class<T> type, Map<String, T> registry) {
        Optional<T> source = getRecordsDAO(sourceId, registry);
        if (!source.isPresent()) {
            throw new IllegalArgumentException("RecordsDAO is not found! Class: " + type + " Id: " + sourceId);
        }
        return source.get();
    }

    @Override
    public void setPredicateService(PredicateService predicateService) {
        this.predicateService = predicateService;
        execAllDaoSetters(PredicateServiceAware.class, predicateService);
    }

    @Override
    public void setRecordsMetaService(RecordsMetaService recordsMetaService) {
        this.recordsMetaService = recordsMetaService;
        execAllDaoSetters(RecordsMetaServiceAware.class, recordsMetaService);
    }

    @Override
    public void setRecordsService(RecordsService recordsService) {
        this.recordsService = recordsService;
        execAllDaoSetters(RecordsServiceAware.class, recordsService);
    }

    private <T> void execAllDaoSetters(Class<?> awareClass, T value) {
        forEachDao(dao -> execDaoSetter(dao, awareClass, value));
    }

    private <T> void execDaoSetter(RecordsDAO dao, Class<?> awareClass, T value) {

        if (value == null) {
            return;
        }

        Method[] methods = awareClass.getDeclaredMethods();
        if (methods.length == 1
                && methods[0].getParameterCount() == 1
                && methods[0].getParameterTypes()[0].isInstance(value)) {

            Method method = methods[0];

            if (awareClass.isInstance(dao)) {
                try {
                    method.invoke(dao, value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Error", e);
                }
            }
        }
    }

    private void forEachDao(Consumer<RecordsDAO> action) {

        Map<RecordsDAO, Boolean> visited = new IdentityHashMap<>();

        Consumer<RecordsDAO> singleVisitAction = dao -> {
            if (visited.put(dao, true) == null) {
                action.accept(dao);
            }
        };

        metaDAO.values().forEach(singleVisitAction);
        queryDAO.values().forEach(singleVisitAction);
        mutableDAO.values().forEach(singleVisitAction);
        queryWithMetaDAO.values().forEach(singleVisitAction);
    }
}
