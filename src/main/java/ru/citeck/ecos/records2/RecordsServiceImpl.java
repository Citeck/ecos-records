package ru.citeck.ecos.records2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.model.AndPredicate;
import ru.citeck.ecos.predicate.model.OrPredicate;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.Predicates;
import ru.citeck.ecos.querylang.QueryLangService;
import ru.citeck.ecos.querylang.QueryWithLang;
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
import ru.citeck.ecos.records2.source.common.group.RecordsGroupDAO;
import ru.citeck.ecos.records2.source.dao.*;
import ru.citeck.ecos.records2.utils.RecordsUtils;
import ru.citeck.ecos.records2.utils.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RecordsServiceImpl extends AbstractRecordsService {

    private static final String DEBUG_QUERY_TIME = "queryTimeMs";
    private static final String DEBUG_RECORDS_QUERY_TIME = "recordsQueryTimeMs";
    private static final String DEBUG_META_QUERY_TIME = "metaQueryTimeMs";
    private static final String DEBUG_META_SCHEMA = "schema";

    private static final Pattern ATT_PATTERN = Pattern.compile("^\\.atts?\\(n:\"([^\"]+)\"\\).+");

    private static final Log logger = LogFactory.getLog(RecordsServiceImpl.class);

    private Map<String, RecordsMetaDAO> metaDAO = new ConcurrentHashMap<>();
    private Map<String, RecordsQueryDAO> queryDAO = new ConcurrentHashMap<>();
    private Map<String, MutableRecordsDAO> mutableDAO = new ConcurrentHashMap<>();
    private Map<String, RecordsQueryWithMetaDAO> queryWithMetaDAO = new ConcurrentHashMap<>();

    private RecordsMetaService recordsMetaService;
    private PredicateService predicateService;
    private QueryLangService queryLangService;

    public RecordsServiceImpl(RecordsMetaService recordsMetaService,
                              PredicateService predicateService,
                              QueryLangService queryLangService) {
        this.recordsMetaService = recordsMetaService;
        this.predicateService = predicateService;
        this.queryLangService = queryLangService;
    }

    /* QUERY */

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

        logger.warn("RecordsDAO " + query.getSourceId() + " doesn't exists or "
                    + "doesn't implement RecordsQueryDAO or RecordsQueryWithMetaDAO");

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
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Collection<String> attributes) {
        return queryRecords(query, toAttributesMap(attributes));
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes) {

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        RecordsQueryResult<RecordMeta> records = queryRecords(query, schema.getSchema());
        records.setRecords(recordsMetaService.convertToFlatMeta(records.getRecords(), schema));

        return records;
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

                distinctPredicate.addPredicate(
                        Predicates.equal(distinctQuery.getAttribute(),
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

        Optional<RecordsQueryWithMetaDAO> recordsDAO = getRecordsDAO(query.getSourceId(), queryWithMetaDAO);
        Optional<RecordsQueryDAO> recordsQueryDAO = getRecordsDAO(query.getSourceId(), queryDAO);

        RecordsQueryResult<RecordMeta> records;

        RecordsQuery convertedQuery = updateQueryLanguage(query, recordsDAO.orElse(null));

        if (convertedQuery != null) {

            if (logger.isDebugEnabled()) {
                logger.debug("Start records with meta query: " + convertedQuery.getQuery() + "\n" + schema);
            }

            long queryStart = System.currentTimeMillis();
            records = recordsDAO.get().queryRecords(convertedQuery, schema);
            long queryDuration = System.currentTimeMillis() - queryStart;

            if (logger.isDebugEnabled()) {
                logger.debug("Stop records with meta query. Duration: " + queryDuration);
            }

            if (query.isDebug()) {
                records.setDebugInfo(getClass(), DEBUG_QUERY_TIME, queryDuration);
            }

        } else  {

            convertedQuery = updateQueryLanguage(query, recordsQueryDAO.orElse(null));

            if (convertedQuery == null) {

                records = new RecordsQueryResult<>();
                if (query.isDebug()) {
                    records.setDebugInfo(getClass(),
                            "RecordsDAO",
                            "Source with id '" + query.getSourceId()
                                    + "' is not found or language is not supported");
                }
            } else {

                if (logger.isDebugEnabled()) {
                    logger.debug("Start records query: " + convertedQuery.getQuery());
                }

                long recordsQueryStart = System.currentTimeMillis();
                RecordsQueryResult<RecordRef> recordRefs = recordsQueryDAO.get().queryRecords(convertedQuery);
                long recordsTime = System.currentTimeMillis() - recordsQueryStart;

                if (logger.isDebugEnabled()) {
                    int found = recordRefs.getRecords().size();
                    logger.debug("Stop records query. Found: " + found + " Duration: " + recordsTime);
                    logger.debug("Start meta query: " + schema);
                }

                records = new RecordsQueryResult<>();
                records.merge(recordRefs);
                records.setTotalCount(recordRefs.getTotalCount());

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

    /* ATTRIBUTES */

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
    public RecordMeta getAttributes(RecordRef record, Collection<String> attributes) {

        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordMeta getAttributes(RecordRef record, Map<String, String> attributes) {

        return extractOne(getAttributes(Collections.singletonList(record), attributes), record);
    }

    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records,
                                                   Collection<String> attributes) {

        return getAttributes(records, toAttributesMap(attributes));
    }

    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records,
                                                   Map<String, String> attributes) {

        if (attributes.isEmpty()) {
            return new RecordsResult<>(new ArrayList<>(records), RecordMeta::new);
        }

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        RecordsResult<RecordMeta> meta = getMeta(records, schema.getSchema());
        meta.setRecords(recordsMetaService.convertToFlatMeta(meta.getRecords(), schema));

        return meta;
    }

    /* META */

    @Override
    public <T> T getMeta(RecordRef recordRef, Class<T> metaClass) {

        RecordsResult<T> meta = getMeta(Collections.singletonList(recordRef), metaClass);
        if (meta.getRecords().size() == 0) {
            throw new IllegalStateException("Can't get record metadata. Result: " + meta);
        }
        return meta.getRecords().get(0);
    }

    @Override
    public <T> RecordsResult<T> getMeta(Collection<RecordRef> records, Class<T> metaClass) {

        Map<String, String> attributes = recordsMetaService.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            logger.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        RecordsResult<RecordMeta> meta = getAttributes(records, attributes);

        return new RecordsResult<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {

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

        return results;
    }

    /* MODIFICATION */

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        Map<String, RecordRef> aliasToRecordRef = new HashMap<>();
        RecordsMutResult result = new RecordsMutResult();

        List<RecordMeta> records = mutation.getRecords();

        for (int i = records.size() - 1; i >= 0; i--) {

            RecordMeta record = records.get(i);

            ObjectNode attributes = JsonNodeFactory.instance.objectNode();

            record.forEach((name, value) -> {

                String simpleName = name;

                if (name.charAt(0) != '.') {

                    int dotIdx = name.indexOf('.', 1);

                    if (dotIdx > 0) {
                        simpleName = name.substring(0, dotIdx);
                    } else {
                        int questionIdx = name.indexOf('?');
                        if (questionIdx > 0) {
                            simpleName = name.substring(0, questionIdx);
                        }
                    }

                } else {

                    Matcher matcher = ATT_PATTERN.matcher(name);
                    if (matcher.matches()) {
                        simpleName = matcher.group(1);
                    } else {
                        simpleName = null;
                    }
                }

                if (StringUtils.isNotBlank(simpleName)) {

                    if (name.endsWith("?assoc") || name.endsWith("{assoc}")) {
                        value = convertAssocValue(value, aliasToRecordRef);
                    }

                    attributes.put(simpleName, value);
                }
            });

            record.setAttributes(attributes);

            RecordsMutation sourceMut = new RecordsMutation();
            sourceMut.setRecords(Collections.singletonList(record));
            MutableRecordsDAO dao = needRecordsDAO(record.getId().getSourceId(), MutableRecordsDAO.class, mutableDAO);
            RecordsMutResult recordMutResult = dao.mutate(sourceMut);

            if (i == 0) {
                result.merge(recordMutResult);
            }

            List<RecordMeta> resultRecords = recordMutResult.getRecords();
            for (RecordMeta resultMeta : resultRecords) {
                String alias = record.get(RecordConstants.ATT_ALIAS, "");
                if (StringUtils.isNotBlank(alias)) {
                    aliasToRecordRef.put(alias, resultMeta.getId());
                }
            }
        }

        return result;
    }

    private JsonNode convertAssocValue(JsonNode value, Map<String, RecordRef> mapping) {
        if (value.isTextual()) {
            String textValue = value.asText();
            if (mapping.containsKey(textValue)) {
                return TextNode.valueOf(mapping.get(textValue).toString());
            }
        } else if (value.isArray()) {
            ArrayNode convertedValue = JsonNodeFactory.instance.arrayNode();
            for (JsonNode node : value) {
                convertedValue.add(convertAssocValue(node, mapping));
            }
            return convertedValue;
        }
        return value;
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

    /* OTHER */

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
        if (recordsSource instanceof PredicateServiceAware) {
            ((PredicateServiceAware) recordsSource).setPredicateService(predicateService);
        }
    }



    private <T extends RecordsDAO> void register(Map<String, T> map, Class<T> type, RecordsDAO value) {
        if (type.isAssignableFrom(value.getClass())) {
            @SuppressWarnings("unchecked")
            T dao = (T) value;
            map.put(value.getId(), dao);
        }
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

    private Map<String, String> toAttributesMap(Collection<String> attributes) {
        Map<String, String> attributesMap = new HashMap<>();
        for (String attribute : attributes) {
            attributesMap.put(attribute, attribute);
        }
        return attributesMap;
    }

    protected <T extends RecordsDAO> Optional<T> getRecordsDAO(String sourceId, Map<String, T> registry) {
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

    public PredicateService getPredicateService() {
        return predicateService;
    }

    public RecordsMetaService getRecordsMetaService() {
        return recordsMetaService;
    }

    public QueryLangService getQueryLangService() {
        return queryLangService;
    }
}
