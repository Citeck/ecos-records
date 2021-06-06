package ru.citeck.ecos.records2;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.RecordsDao;

import java.util.Collection;
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
 * <p>Each level converts to a schema and sends to RecordsMetaDao or RecordsQueryWithMetaDao.
 * RecordsMetaService can be used to retrieve data by schema from MetaValue or any other java objects. <br><br>
 *
 * <p>A result of each method to request attributes converts data to a flat view.
 * It means that {"a":{"b":{"c":"value"}}} will be replaced with {"a": "value"}
 *
 * @see MetaValue
 * @see RecordRef
 * @see RecordMeta
 *
 * @deprecated use RecordsService from records3 package
 *
 * @author Pavel Simonov
 */
@Deprecated
public interface RecordsService {

    /* QUERY RECORD */

    /**
     * Query single record.
     *
     * @see RecordsService#queryRecords(RecordsQuery)
     */
    @NotNull
    Optional<RecordRef> queryRecord(RecordsQuery query);

    /**
     * Query single record.
     *
     * @see RecordsService#queryRecords(RecordsQuery, Class)
     */
    @NotNull
    <T> Optional<T> queryRecord(RecordsQuery query, Class<T> metaClass);

    /**
     * Query single record.
     *
     * @see RecordsService#queryRecords(RecordsQuery, Collection)
     */
    @NotNull
    Optional<RecordMeta> queryRecord(RecordsQuery query, Collection<String> attributes);

    /**
     * Query single record.
     *
     * @see RecordsService#queryRecords(RecordsQuery, Map)
     */
    @NotNull
    Optional<RecordMeta> queryRecord(RecordsQuery query, Map<String, String> attributes);

    /* QUERY RECORDS */

    /**
     * Query records.
     *
     * @return list of records, page info and debug info if query.isDebug() returns true
     */
    @NotNull
    RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query);

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
    @NotNull
    <T> RecordsQueryResult<T> queryRecords(RecordsQuery query, Class<T> metaClass);

    /**
     * Query records and its attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Collection<String> attributes);

    /**
     * Query records and its attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes);

    /* ATTRIBUTES */

    /**
     * Get a single record attribute.
     *
     * @return flat record attribute value
     */
    @NotNull
    DataValue getAtt(RecordRef record, String attribute);

    /**
     * Get a single record attribute.
     *
     * @return flat record attribute value
     */
    @NotNull
    DataValue getAttribute(RecordRef record, String attribute);

    /**
     * Get record attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordMeta getAttributes(RecordRef record, Collection<String> attributes);

    /**
     * Get record attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordMeta getAttributes(RecordRef record, Map<String, String> attributes);

    /**
     * Get records attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records, Collection<String> attributes);

    /**
     * Get records attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records, Map<String, String> attributes);

    /**
     * Get raw (not flat) record meta attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     */
    @NotNull
    RecordMeta getRawAttributes(RecordRef record, Map<String, String> attributes);

    /**
     * Get raw (not flat) records meta attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     */
    @NotNull
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
    @NotNull
    <T> T getMeta(@NotNull RecordRef recordRef, @NotNull Class<T> metaClass);

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
    @NotNull
    <T> RecordsResult<T> getMeta(@NotNull Collection<RecordRef> records, @NotNull Class<T> metaClass);

    /* MODIFICATION */

    /**
     * Create or change records.
     */
    @NotNull
    RecordsMutResult mutate(RecordsMutation mutation);

    @NotNull
    RecordMeta mutate(RecordMeta meta);

    /**
     * Delete records.
     */
    @NotNull
    RecordsDelResult delete(RecordsDeletion deletion);

    void register(@NotNull RecordsDao recordsDao);

    void unregister(String sourceId);

    void register(@NotNull String sourceId, @NotNull RecordsDao recordsDao);
}
