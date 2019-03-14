package ru.citeck.ecos.records2;

import com.fasterxml.jackson.databind.JsonNode;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.RecordsDAO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service to work with some abstract "records" from any source.
 * It may be alfresco nodes, database records, generated data and so on
 *
 * @see MetaValue
 * @see RecordRef
 * @see RecordMeta
 *
 * @author Pavel Simonov
 */
public interface RecordsService {

    String LANGUAGE_PREDICATE = "predicate";

    /**
     * Query records from default RecordsDAO.
     * @return list of RecordRef and page info
     */
    RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query);

    /**
     * Query records with meta.
     * @param metaClass POJO to generate metadata GQL schema and retrieve data
     *                  This class must contain constructor without arguments and have public fields
     *                  Getters/setters is not yet supported
     */
    <T> RecordsQueryResult<T> queryRecords(RecordsQuery query, Class<T> metaClass);

    /**
     * Query records with meta.
     * Fields example: {name: 'cm:name', title: 'cm:title'}
     */
    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes);

    /**
     * Query records with meta.
     * Fields example: {name: 'cm:name', title: 'cm:title'}
     */
    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema);

    /**
     * Query records with meta.
     * Fields example: ['cm:name', 'cm:title']
     */
    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Collection<String> attributes);

    /**
     * Get attribute.
     */
    JsonNode getAttribute(RecordRef record, String attribute);

    /**
     * Get meta.
     * Fields example: ["cm:name", "cm:title"]
     */
    RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records, Collection<String> attributes);

    /**
     * Get ordered meta.
     * Fields example: ["cm:name", "cm:title"]
     */
    RecordsResult<RecordMeta> getAttributes(List<RecordRef> records, Collection<String> attributes);

    /**
     * Get meta.
     * Fields example: {"name" : "cm:name", "title" : "cm:title"}
     */
    RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records, Map<String, String> attributes);

    /**
     * Get meta.
     * Fields example: {"name" : "cm:name", "title" : "cm:title"}
     */
    RecordMeta getAttributes(RecordRef record, Map<String, String> attributes);

    /**
     * Get meta.
     * Fields example: {"name" : "cm:name", "title" : "cm:title"}
     */
    RecordMeta getAttributes(RecordRef record, Collection<String> attributes);

    /**
     * Get ordered meta.
     * Fields example: {"name" : "cm:name", "title" : "cm:title"]
     */
    RecordsResult<RecordMeta> getAttributes(List<RecordRef> records, Map<String, String> attributes);

    /**
     * Get ordered meta.
     * Fields example: ["cm:name", "cm:title"]
     */
    RecordsResult<RecordMeta> getMeta(List<RecordRef> records, String schema);

    /**
     * Get metadata for specified records.
     * @param metaClass POJO to generate metadata GQL schema and retrieve data
     *                  This class must contain constructor without arguments and have public fields
     *                  Getters/setters is not yet supported
     *
     * @see MetaAtt
     */
    <T> RecordsResult<T> getMeta(Collection<RecordRef> records, Class<T> metaClass);

    /**
     * Get metadata for specified records.
     * @param metaClass POJO to generate metadata GQL schema and retrieve data
     *
     * @see MetaAtt
     */
    <T> RecordsResult<T> getMeta(List<RecordRef> records, Class<T> metaClass);

    <T> T getMeta(RecordRef recordRef, Class<T> metaClass);

    /**
     * Create or change records.
     */
    RecordsMutResult mutate(RecordsMutation mutation);

    /**
     * Delete records.
     */
    RecordsDelResult delete(RecordsDeletion deletion);

    /**
     * Get Iterable with records which fit the query from default source.
     * This method can be used to process all records in system without search limits
     */
    Iterable<RecordRef> getIterableRecords(RecordsQuery query);

    JsonNode convertQueryLanguage(JsonNode query, String fromLang, String toLang);

    /**
     * Register new RecordsDAO. It must return valid id from method "getId()" to call this method.
     */
    void register(RecordsDAO recordsSource);

    void register(QueryLangConverter converter, String fromLang, String toLang);

    // Deprecated methods

    /**
     * queryRecords.
     *
     * @deprecated use queryRecords instead
     */
    @Deprecated
    default RecordsQueryResult<RecordRef> getRecords(RecordsQuery query) {
        return queryRecords(query);
    }

    /**
     * queryRecords.
     *
     * @deprecated use queryRecords instead
     */
    @Deprecated
    default <T> RecordsQueryResult<T> getRecords(RecordsQuery query, Class<T> metaClass) {
        return queryRecords(query, metaClass);
    }

    /**
     * queryRecords.
     *
     * @deprecated use queryRecords instead
     */
    @Deprecated
    default RecordsQueryResult<RecordMeta> getRecords(RecordsQuery query, Map<String, String> attributes) {
        return queryRecords(query, attributes);
    }

    /**
     * queryRecords.
     *
     * @deprecated use queryRecords instead
     */
    @Deprecated
    default RecordsQueryResult<RecordMeta> getRecords(RecordsQuery query, String schema) {
        return queryRecords(query, schema);
    }

    /**
     * queryRecords.
     *
     * @deprecated use queryRecords instead
     */
    @Deprecated
    default RecordsQueryResult<RecordMeta> getRecords(RecordsQuery query, Collection<String> attributes) {
        return queryRecords(query, attributes);
    }
}
