package ru.citeck.ecos.records2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.model.AndPredicate;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.ValuePredicate;
import ru.citeck.ecos.records2.meta.AttributesSchema;
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
import ru.citeck.ecos.records2.source.dao.*;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RecordsServiceImpl implements RecordsService {

    private static final String DEBUG_QUERY_TIME = "queryTimeMs";
    private static final String DEBUG_RECORDS_QUERY_TIME = "recordsQueryTimeMs";
    private static final String DEBUG_META_QUERY_TIME = "metaQueryTimeMs";
    private static final String DEBUG_META_SCHEMA = "schema";

    private static final Log logger = LogFactory.getLog(RecordsServiceImpl.class);

    private Map<String, RecordsMetaDAO> metaDAO = new ConcurrentHashMap<>();
    private Map<String, RecordsQueryDAO> queryDAO = new ConcurrentHashMap<>();
    private Map<String, MutableRecordsDAO> mutableDAO = new ConcurrentHashMap<>();
    private Map<String, RecordsQueryWithMetaDAO> queryWithMetaDAO = new ConcurrentHashMap<>();

    private RecordsMetaService recordsMetaService;
    private PredicateService predicateService;

    private Map<LangConvPair, QueryLangConverter> languageConverters = new ConcurrentHashMap<>();

    public RecordsServiceImpl(RecordsMetaService recordsMetaService, PredicateService predicateService) {
        this.recordsMetaService = recordsMetaService;
        this.predicateService = predicateService;
    }

    @Override
    public RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query) {

        Optional<RecordsQueryDAO> recordsQueryDAO = getRecordsDAO(query.getSourceId(), queryDAO);

        if (recordsQueryDAO.isPresent()) {

            return recordsQueryDAO.get().queryRecords(query);

        } else {

            Optional<RecordsQueryWithMetaDAO> recordsWithMetaDAO = getRecordsDAO(query.getSourceId(), queryWithMetaDAO);

            if (recordsWithMetaDAO.isPresent()) {

                RecordsQueryResult<RecordMeta> records = recordsWithMetaDAO.get().queryRecords(query, "");
                return new RecordsQueryResult<>(records, RecordMeta::getId);
            }
        }

        logger.warn("RecordsDAO " + query.getSourceId() + " doesn't exists or " +
                    "doesn't implement RecordsQueryDAO or RecordsQueryWithMetaDAO");

        return new RecordsQueryResult<>();
    }

    @Override
    public <T> RecordsQueryResult<T> queryRecords(RecordsQuery query, Class<T> metaClass) {

        Map<String, String> attributes = recordsMetaService.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Meta class doesn't has any fields with setter. Class: " + metaClass);
        }

        RecordsQueryResult<RecordMeta> meta = queryRecords(query, attributes);

        return new RecordsQueryResult<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes) {

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        RecordsQueryResult<RecordMeta> records = queryRecords(query, schema.getSchema());
        records.setRecords(recordsMetaService.convertToFlatMeta(records.getRecords(), schema));

        return records;
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query,
                                                       Collection<String> attributes) {
        return queryRecords(query, toAttributesMap(attributes));
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {

        if (!query.getGroupBy().isEmpty()) {

            QueryLangConverter toPredicate;

            if (query.getLanguage().equals(LANGUAGE_PREDICATE)) {

                toPredicate = q -> q;

            } else {

                toPredicate = languageConverters.get(new LangConvPair(query.getLanguage(), LANGUAGE_PREDICATE));
            }

            if (toPredicate != null) {
                return queryRecordsGroups(query, schema, toPredicate);
            } else {
                logger.warn("GroupBy is not supported. Query: " + query);
                return new RecordsQueryResult<>();
            }
        } else {

            getRecordsDAO(query.getSourceId(), queryWithMetaDAO);
        }

        return queryRecordsImpl(query, schema);
    }

    private boolean updateQueryLanguage(RecordsQuery recordsQuery, RecordsQueryBaseDAO dao) {

        List<String> supportedLanguages = dao.getSupportedLanguages();

        if (supportedLanguages != null && supportedLanguages.size() > 0) {

            if (!supportedLanguages.contains(recordsQuery.getLanguage())) {

                for (String daoLanguage : supportedLanguages) {

                    LangConvPair langConvKey = new LangConvPair(recordsQuery.getLanguage(), daoLanguage);
                    QueryLangConverter langConv = languageConverters.get(langConvKey);
                    JsonNode query = langConv != null ? langConv.convert(recordsQuery.getQuery()) : null;

                    if (query != null) {

                        recordsQuery.setQuery(query);
                        recordsQuery.setLanguage(daoLanguage);

                        return true;
                    }
                }
            }

            return false;
        }
        return true;
    }

    private RecordsQueryResult<RecordMeta> queryRecordsImpl(RecordsQuery query, String schema) {

        Optional<RecordsQueryWithMetaDAO> recordsDAO = getRecordsDAO(query.getSourceId(), queryWithMetaDAO);
        RecordsQueryResult<RecordMeta> records;

        if (recordsDAO.isPresent() && updateQueryLanguage(query, recordsDAO.get())) {

            updateQueryLanguage(query, recordsDAO.get());

            if (logger.isDebugEnabled()) {
                logger.debug("Start records with meta query: " + query.getQuery() + "\n" + schema);
            }

            long queryStart = System.currentTimeMillis();
            records = recordsDAO.get().queryRecords(query, schema);
            long queryDuration = System.currentTimeMillis() - queryStart;

            if (logger.isDebugEnabled()) {
                logger.debug("Stop records with meta query. Duration: " + queryDuration);
            }

            if (query.isDebug()) {
                records.setDebugInfo(getClass(), DEBUG_QUERY_TIME, queryDuration);
            }

        } else  {

            Optional<RecordsQueryDAO> recordsQueryDAO = getRecordsDAO(query.getSourceId(), queryDAO);

            if (!recordsQueryDAO.isPresent() || !updateQueryLanguage(query, recordsQueryDAO.get())) {

                records = new RecordsQueryResult<>();
                if (query.isDebug()) {
                    records.setDebugInfo(getClass(),
                        "RecordsDAO",
                        "Source with id '" + query.getSourceId() + "' is not found or language is not supported");
                }
            } else {

                if (logger.isDebugEnabled()) {
                    logger.debug("Start records query: " + query.getQuery());
                }

                long recordsQueryStart = System.currentTimeMillis();
                RecordsQueryResult<RecordRef> recordRefs = recordsQueryDAO.get().queryRecords(query);
                long recordsTime = System.currentTimeMillis() - recordsQueryStart;

                if (logger.isDebugEnabled()) {
                    int found = recordRefs.getRecords().size();
                    logger.debug("Stop records query. Found: " + found + " Duration: " + recordsTime);
                    logger.debug("Start meta query: " + schema);
                }

                records = new RecordsQueryResult<>();
                records.merge(recordRefs);
                records.setTotalCount(recordRefs.getTotalCount());
                records.setHasMore(recordRefs.getHasMore());

                long metaQueryStart = System.currentTimeMillis();
                records.merge(getMeta(recordRefs.getRecords(), schema));
                long metaTime = System.currentTimeMillis() - metaQueryStart;

                if (logger.isDebugEnabled()) {
                    logger.debug("Stop meta query. Duration: " + metaTime);
                }

                if (query.isDebug()) {
                    records.setDebugInfo(getClass(), DEBUG_RECORDS_QUERY_TIME, recordsTime);
                    records.setDebugInfo(getClass(), DEBUG_META_QUERY_TIME, metaTime);
                }
            }
        }

        if (query.isDebug()) {
            records.setDebugInfo(getClass(), DEBUG_META_SCHEMA, schema);
        }

        return records;
    }

    private RecordsQueryResult<RecordMeta> queryRecordsGroups(RecordsQuery query,
                                                              String schema,
                                                              QueryLangConverter toPredicate) {

        List<String> groupBy = query.getGroupBy();

        String attribute = groupBy.get(0);

        RecordsQuery distinctQuery = new RecordsQuery(query);
        distinctQuery.setMaxItems(20);
        distinctQuery.setGroupBy(null);
        distinctQuery.setQuery(DistinctQuery.create(attribute, query.getQuery(), query.getLanguage()));
        distinctQuery.setLanguage(DistinctQuery.LANGUAGE);

        RecordsQueryResult<RecordMeta> distinctValuesResult = queryRecordsImpl(distinctQuery, "str");

        if (distinctValuesResult.getRecords().isEmpty()) {
            return new RecordsQueryResult<>();
        }

        RecordsQuery groupsBaseQuery = new RecordsQuery(query);
        if (groupBy.size() == 1) {
            groupsBaseQuery.setGroupBy(null);
        } else {
            List<String> newGroupBy = new ArrayList<>();
            for (int i = 1; i < groupBy.size(); i++) {
                newGroupBy.add(groupBy.get(i));
            }
            groupsBaseQuery.setGroupBy(newGroupBy);
        }

        JsonNode predicateQuery = toPredicate.convert(groupsBaseQuery.getQuery());
        Predicate predicate = predicateService.readJson(predicateQuery);

        List<RecordsGroup> groups = new ArrayList<>();

        for (RecordMeta meta : distinctValuesResult.getRecords()) {

            Predicate groupPredicate = ValuePredicate.equal(attribute, meta.get("str", ""));
            RecordsQuery groupQuery = new RecordsQuery(groupsBaseQuery);

            groupQuery.setQuery(predicateService.writeJson(AndPredicate.of(predicate, groupPredicate)));
            groupQuery.setLanguage(LANGUAGE_PREDICATE);

            groups.add(new RecordsGroup(groupQuery, groupPredicate, this));
        }

        RecordsQueryResult<RecordMeta> result = new RecordsQueryResult<>();
        result.merge(recordsMetaService.getMeta(groups, schema));

        return result;
    }

    @Override
    public <T> RecordsResult<T> getMeta(List<RecordRef> records, Class<T> metaClass) {

        Map<String, String> attributes = recordsMetaService.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            logger.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        RecordsResult<RecordMeta> meta = getAttributes(records, attributes);

        return new RecordsResult<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records,
                                                   Collection<String> attributes) {

        return getAttributes(new ArrayList<>(records), attributes);
    }

    @Override
    public RecordsResult<RecordMeta> getAttributes(List<RecordRef> records,
                                                   Collection<String> attributes) {
        return getAttributes(records, toAttributesMap(attributes));
    }

    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records,
                                                   Map<String, String> attributes) {

        return getAttributes(new ArrayList<>(records), attributes);
    }

    @Override
    public RecordMeta getAttributes(RecordRef record, Map<String, String> attributes) {

        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordMeta getAttributes(RecordRef record, Collection<String> attributes) {

        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    private RecordMeta extractOne(RecordsResult<RecordMeta> values, RecordRef record) {

        if (values.getRecords().isEmpty()) {
            return new RecordMeta(record);
        }
        RecordMeta meta = values.getRecords()
                                .stream()
                                .filter(r -> record.equals(r.getId()))
                                .findFirst()
                                .orElse(null);
        if (meta == null) {
            meta = new RecordMeta(record);
        }
        return meta;
    }

    @Override
    public <T> T getMeta(RecordRef recordRef, Class<T> metaClass) {

        RecordsResult<T> meta = getMeta(Collections.singletonList(recordRef), metaClass);
        if (meta.getRecords().size() == 0) {
            throw new IllegalStateException("Can't get record metadata. Result: " + meta);
        }
        return meta.getRecords().get(0);
    }

    @Override
    public <T> RecordsResult<T> getMeta(Collection<RecordRef> records,
                                        Class<T> metaClass) {

        return getMeta(new ArrayList<>(records), metaClass);
    }

    @Override
    public RecordsResult<RecordMeta> getAttributes(List<RecordRef> records,
                                                   Map<String, String> attributes) {

        if (attributes.isEmpty()) {
            return new RecordsResult<>(records, RecordMeta::new);
        }

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        RecordsResult<RecordMeta> meta = getMeta(records, schema.getSchema());
        meta.setRecords(recordsMetaService.convertToFlatMeta(meta.getRecords(), schema));

        return meta;
    }

    @Override
    public JsonNode getAttribute(RecordRef record, String attribute) {
        RecordsResult<RecordMeta> meta = getAttributes(Collections.singletonList(record),
                                                       Collections.singletonList(attribute));
        if (!meta.getRecords().isEmpty()) {
            return meta.getRecords().get(0).getAttribute(attribute);
        }
        return MissingNode.getInstance();
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(List<RecordRef> records, String schema) {

        RecordsResult<RecordMeta> results = new RecordsResult<>();

        RecordsUtils.groupRefBySource(records).forEach((sourceId, recs) -> {

            Optional<RecordsMetaDAO> recordsDAO = getRecordsDAO(sourceId, metaDAO);
            RecordsResult<RecordMeta> meta;

            if (recordsDAO.isPresent()) {

                meta = recordsDAO.get().getMeta(records, schema);

            } else {

                meta = new RecordsResult<>();
                meta.setRecords(recs.stream().map(RecordMeta::new).collect(Collectors.toList()));
                logger.error("Records source " + sourceId + " can't return attributes");
            }

            results.merge(meta);
        });

        return results;
    }

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        for (RecordMeta record : mutation.getRecords()) {

            ObjectNode attributes = JsonNodeFactory.instance.objectNode();

            record.forEach((name, value) -> {

                if (name.charAt(0) != '.') {

                    int qIdx = name.indexOf('?');
                    if (qIdx > 0) {
                        name = name.substring(0, qIdx);
                    }

                    attributes.put(name, value);
                }
            });

            record.setAttributes(attributes);
        }

        RecordsMutResult result = new RecordsMutResult();

        RecordsUtils.groupMetaBySource(mutation.getRecords()).forEach((sourceId, records) -> {

            RecordsMutation sourceMut = new RecordsMutation();
            sourceMut.setRecords(records);

            MutableRecordsDAO dao = needRecordsDAO(sourceId, MutableRecordsDAO.class, mutableDAO);
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

    @Override
    public Iterable<RecordRef> getIterableRecords(RecordsQuery query) {
        return new IterableRecords(this, query);
    }

    @Override
    public void register(RecordsDAO recordsSource) {

        String id = recordsSource.getId();
        if (id == null) {
            throw new IllegalArgumentException("id is a mandatory parameter for RecordsDAO");
        }

        register(metaDAO, RecordsMetaDAO.class, recordsSource);
        register(queryDAO, RecordsQueryDAO.class, recordsSource);
        register(mutableDAO, MutableRecordsDAO.class, recordsSource);
        register(queryWithMetaDAO, RecordsQueryWithMetaDAO.class, recordsSource);

        if (recordsSource instanceof RecordsServiceAware) {
            ((RecordsServiceAware) recordsSource).setRecordsService(this);
        }
        if (recordsSource instanceof RecordsMetaServiceAware) {
            ((RecordsMetaServiceAware) recordsSource).setRecordsMetaService(recordsMetaService);
        }
    }

    @Override
    public void register(QueryLangConverter converter, String fromLang, String toLang) {
        languageConverters.put(new LangConvPair(fromLang, toLang), converter);
    }

    private <T extends RecordsDAO> void register(Map<String, T> map, Class<T> type, RecordsDAO value) {
        if (type.isAssignableFrom(value.getClass())) {
            @SuppressWarnings("unchecked")
            T dao = (T) value;
            map.put(value.getId(), dao);
        }
    }

    public PredicateService getPredicateService() {
        return predicateService;
    }

    public RecordsMetaService getRecordsMetaService() {
        return recordsMetaService;
    }

    private Map<String, String> toAttributesMap(Collection<String> attributes) {
        Map<String, String> attributesMap = new HashMap<>();
        for (String attribute : attributes) {
            attributesMap.put(attribute, attribute);
        }
        return attributesMap;
    }

    protected  <T extends RecordsDAO> Optional<T> getRecordsDAO(String sourceId, Map<String, T> registry) {
        if (sourceId == null) {
            sourceId = "";
        }
        return Optional.ofNullable(registry.get(sourceId));
    }

    protected <T extends RecordsDAO> T needRecordsDAO(String sourceId, Class<T> type, Map<String, T> registry) {
        Optional<T> source = getRecordsDAO(sourceId, registry);
        if (!source.isPresent()) {
            throw new IllegalArgumentException("RecordsDAO is not found! Class: " + type + " Id: " + sourceId);
        }
        return source.get();
    }

    private static class LangConvPair {

        private final String from;
        private final String to;

        public LangConvPair(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LangConvPair langPair = (LangConvPair) o;
            return Objects.equals(from, langPair.from) &&
                   Objects.equals(to, langPair.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }
}
