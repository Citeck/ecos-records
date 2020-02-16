package ru.citeck.ecos.records2;

import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.objdata.DataValue;
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
import java.util.Optional;

/**
 * Service to work with some abstract "records" from any source.
 * It may be database records, generated data and so on.
 * Each record can be identified by its RecordRef. <br><br>
 *
 * <p>There are three main purposes: <br>
 * 1) Query records by some language, query and other parameters; <br>
 * 2) Get records metadata. The metadata in this context is any record data (e.g. document status or title); <br>
 * 3) Delete, create or modify records. <br><br>
 *
 * <p>There are three levels of abstraction for retrieving metadata: <br>
 * <b>DTO Class > Attributes > Schema</b> <br>
 *
 * <p>Each level converts to a schema and sends to RecordsMetaDAO or RecordsQueryWithMetaDAO.
 * RecordsMetaService can be used to retrieve data by schema from MetaValue or any other java objects. <br><br>
 *
 * <p>A result of each method to request attributes converts data to a flat view.
 * It means that {"a":{"b":{"c":"value"}}} will be replaced with {"a": "value"}
 *
 * @see MetaValue
 * @see RecordRef
 * @see RecordMeta
 * @see RecordsMetaService
 *
 * @author Pavel Simonov
 */
public interface RecordsService {

    /* QUERY RECORD */

    /**
     * Query single record.
     *
     * @see RecordsService#queryRecords(RecordsQuery)
     */
    Optional<RecordRef> queryRecord(RecordsQuery query);

    /**
     * Query single record.
     *
     * @see RecordsService#queryRecords(RecordsQuery, Class)
     */
    <T> Optional<T> queryRecord(RecordsQuery query, Class<T> metaClass);

    /**
     * Query single record.
     *
     * @see RecordsService#queryRecords(RecordsQuery, Collection)
     */
    Optional<RecordMeta> queryRecord(RecordsQuery query, Collection<String> attributes);

    /**
     * Query single record.
     *
     * @see RecordsService#queryRecords(RecordsQuery, Map)
     */
    Optional<RecordMeta> queryRecord(RecordsQuery query, Map<String, String> attributes);

    /* QUERY RECORDS */

    /**
     * Query records.
     *
     * @return list of records, page info and debug info if query.isDebug() returns true
     */
    RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query);

    RecordsQueryResult<List<RecordRef>> queryRecords(List<DataValue> foreach, RecordsQuery query);

    /**
     * Query records with data. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified metaClass will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param query query to search records
     * @param metaClass DTO to generate metadata schema and retrieve data
     *
     * @see MetaAtt
     */
    <T> RecordsQueryResult<T> queryRecords(RecordsQuery query, Class<T> metaClass);

    <T> RecordsQueryResult<List<T>> queryRecords(List<DataValue> foreach, RecordsQuery query, Class<T> metaClass);

    /**
     * Query records and its attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Collection<String> attributes);

    RecordsQueryResult<List<RecordMeta>> queryRecords(List<DataValue> foreach,
                                                      RecordsQuery query,
                                                      Collection<String> attributes);

    /**
     * Query records and its attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes);

    RecordsQueryResult<List<RecordMeta>> queryRecords(List<DataValue> foreach,
                                                      RecordsQuery query,
                                                      Map<String, String> attributes);

    /**
     * Low-level method to search records and receive its metadata.
     * Usually, you should not use this method because it has a set of disadvantages against other methods.
     *
     * @param schema GraphQL schema of metadata to receive
     */
    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema);

    RecordsQueryResult<List<RecordMeta>> queryRecords(List<DataValue> foreach, RecordsQuery query, String schema);

    /* ATTRIBUTES */

    /**
     * Get a single record attribute.
     *
     * @return flat record attribute value
     */
    DataValue getAttribute(RecordRef record, String attribute);

    /**
     * Get record attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    RecordMeta getAttributes(RecordRef record, Collection<String> attributes);

    /**
     * Get record attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    RecordMeta getAttributes(RecordRef record, Map<String, String> attributes);

    /**
     * Get records attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records, Collection<String> attributes);

    /**
     * Get records attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records, Map<String, String> attributes);

    /**
     * Get raw (not flat) record meta attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     */
    RecordMeta getRawAttributes(RecordRef record, Map<String, String> attributes);

    /**
     * Get raw (not flat) records meta attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     */
    RecordsResult<RecordMeta> getRawAttributes(Collection<RecordRef> records, Map<String, String> attributes);

    /* META */

    /**
     * Get record metadata. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified metaClass will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param metaClass DTO to generate metadata schema and retrieve data
     *
     * @see MetaAtt
     */
    <T> T getMeta(RecordRef recordRef, Class<T> metaClass);

    /**
     * Get records metadata. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified metaClass will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param metaClass DTO to generate metadata schema and retrieve data
     *
     * @see MetaAtt
     */
    <T> RecordsResult<T> getMeta(Collection<RecordRef> records, Class<T> metaClass);

    /**
     * Low-level method to get records metadata.
     * Usually, you should not use this method because it has a set of disadvantages against other methods.
     *
     * @param schema GraphQL schema of metadata to receive
     *
     * @return records metadata
     */
    RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema);

    /* MODIFICATION */

    /**
     * Create or change records.
     */
    RecordsMutResult mutate(RecordsMutation mutation);

    RecordMeta mutate(RecordMeta meta);

    /**
     * Delete records.
     */
    RecordsDelResult delete(RecordsDeletion deletion);

    /* OTHER */

    /**
     * Register the RecordsDAO. It must return valid id from method "getId()" to call this method.
     */
    void register(RecordsDAO recordsSource);
}
