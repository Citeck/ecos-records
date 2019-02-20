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
import ru.citeck.ecos.records2.source.MetaAttributeDef;
import ru.citeck.ecos.records2.source.dao.RecordsDAO;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDAO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to work with some abstract "records" from any source
 * It may be alfresco nodes, database records, generated data and so on
 * A record id contains two parts: 'sourceId' and 'id'. String representation: sourceId@id
 *
 * @see MetaValue
 * @see RecordRef
 * @see RecordsQueryDAO
 *
 * @author Pavel Simonov
 */
public interface RecordsService {

    /**
     * Query records from default RecordsDAO
     * @return list of RecordRef and page info
     */
    RecordsQueryResult<RecordRef> getRecords(RecordsQuery query);

    /**
     * Query records with meta
     * @param metaClass POJO to generate metadata GQL schema and retrieve data
     *                  This class must contain constructor without arguments and have public fields
     *                  Getters/setters is not yet supported
     */
    <T> RecordsQueryResult<T> getRecords(RecordsQuery query, Class<T> metaClass);

    /**
     * Query records with meta
     * Fields example: {name: 'cm:name', title: 'cm:title'}
     */
    RecordsQueryResult<RecordMeta> getRecords(RecordsQuery query, Map<String, String> attributes);

    /**
     * Query records with meta
     * Fields example: {name: 'cm:name', title: 'cm:title'}
     */
    RecordsQueryResult<RecordMeta> getRecords(RecordsQuery query, String schema);

    /**
     * Query records with meta
     * Fields example: ['cm:name', 'cm:title']
     */
    RecordsQueryResult<RecordMeta> getRecords(RecordsQuery query, Collection<String> attributes);

    /**
     * Get meta
     * Fields example: ["cm:name", "cm:title"]
     */
    RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records, Collection<String> attributes);

    /**
     * Get attribute
     */
    JsonNode getAttribute(RecordRef record, String attribute);

    /**
     * Get ordered meta
     * Fields example: ["cm:name", "cm:title"]
     */
    RecordsResult<RecordMeta> getAttributes(List<RecordRef> records, Collection<String> attributes);

    /**
     * Get meta
     * Fields example: {"name" : "cm:name", "title" : "cm:title"}
     */
    RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records, Map<String, String> attributes);

    /**
     * Get meta
     * Fields example: {"name" : "cm:name", "title" : "cm:title"}
     */
    RecordMeta getAttributes(RecordRef record, Map<String, String> attributes);

    /**
     * Get meta
     * Fields example: {"name" : "cm:name", "title" : "cm:title"}
     */
    RecordMeta getAttributes(RecordRef record, Collection<String> attributes);

    /**
     * Get ordered meta
     * Fields example: {"name" : "cm:name", "title" : "cm:title"]
     */
    RecordsResult<RecordMeta> getAttributes(List<RecordRef> records, Map<String, String> attributes);

    /**
     * Get ordered meta
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
     * Create or change records
     */
    RecordsMutResult mutate(RecordsMutation mutation);

    /**
     * Delete records
     */
    RecordsDelResult delete(RecordsDeletion deletion);

    /**
     * Get Iterable with records which fit the query from default source.
     * This method can be used to process all records in system without search limits
     */
    Iterable<RecordRef> getIterableRecords(RecordsQuery query);

    /**
     * Register new RecordsDAO. It must return valid id from method "getId()" to call this method.
     */
    void register(RecordsDAO recordsSource);

    List<MetaAttributeDef> getAttributesDef(String sourceId, Collection<String> names);

    Optional<MetaAttributeDef> getAttributeDef(String sourceId, String name);
}
