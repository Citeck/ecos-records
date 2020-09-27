package ru.citeck.ecos.records3;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records3.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.record.operation.delete.DelStatus;
import ru.citeck.ecos.records3.record.operation.meta.RecordAttsService;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.RecordsDao;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;

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
 * <p>Each level converts to a schema and sends to RecordsMetaDao or RecordsQueryWithMetaDao.
 * RecordsMetaService can be used to retrieve data by schema from MetaValue or any other java objects. <br><br>
 *
 * <p>A result of each method to request attributes converts data to a flat view.
 * It means that {"a":{"b":{"c":"value"}}} will be replaced with {"a": "value"}
 *
 * Res == Result
 * Att == Attribute
 * Atts == Attributes
 * Rec == Record
 * Recs == Recordsxfc
 *
 * @see AttValue
 * @see RecordRef
 * @see RecordAtts
 * @see RecordAttsService
 *
 * @author Pavel Simonov
 */
public interface RecordsService {

    /* QUERY RECORD */

    /**
     * Query single record.
     *
     * @see RecordsService#query(RecordsQuery)
     */
    @NotNull
    Optional<RecordRef> queryOne(RecordsQuery query);

    /**
     * Query single record.
     *
     * @see RecordsService#query(RecordsQuery, Class)
     */
    @NotNull
    <T> Optional<T> queryOne(RecordsQuery query, Class<T> metaClass);

    /**
     * Query single record.
     *
     * @see RecordsService#query(RecordsQuery, Collection)
     */
    @NotNull
    Optional<RecordAtts> queryOne(RecordsQuery query, Collection<String> attributes);

    /**
     * Query single record.
     *
     * @see RecordsService#query(RecordsQuery, Collection)
     */
    @NotNull
    Optional<RecordAtts> queryOne(RecordsQuery query, Collection<String> attributes, boolean rawAtts);

    /**
     * Query single record.
     *
     * @see RecordsService#query(RecordsQuery, Map)
     */
    @NotNull
    Optional<RecordAtts> queryOne(RecordsQuery query, Map<String, String> attributes);

    /**
     * Query single record.
     *
     * @see RecordsService#query(RecordsQuery, Map)
     */
    @NotNull
    Optional<RecordAtts> queryOne(RecordsQuery query, Map<String, String> attributes, boolean rawAtts);

    /* QUERY RECORDS */

    /**
     * Query records.
     *
     * @return list of records, page info and debug info if query.isDebug() returns true
     */
    @NotNull
    RecordsQueryRes<RecordRef> query(RecordsQuery query);

    @NotNull
    RecordsQueryRes<List<RecordRef>> query(List<DataValue> foreach, RecordsQuery query);

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
    <T> RecordsQueryRes<T> query(RecordsQuery query, Class<T> metaClass);

    @NotNull
    <T> RecordsQueryRes<List<T>> query(List<DataValue> foreach, RecordsQuery query, Class<T> metaClass);

    /**
     * Query records and its attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordsQueryRes<RecordAtts> query(RecordsQuery query, Collection<String> attributes);

    /**
     * Query records and its attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordsQueryRes<RecordAtts> query(RecordsQuery query, Collection<String> attributes, boolean rawAtts);

    @NotNull
    RecordsQueryRes<List<RecordAtts>> query(List<DataValue> foreach, RecordsQuery query, Collection<String> attributes);
    @NotNull
    RecordsQueryRes<List<RecordAtts>> query(List<DataValue> foreach, RecordsQuery query, Collection<String> attributes,
                                            boolean rawAtts);

    /**
     * Query records and its attributes.
     *
     * @param attributes map, where a key is a alias for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordsQueryRes<RecordAtts> query(RecordsQuery query, Map<String, String> attributes);

    /**
     * Query records and its attributes.
     *
     * @param attributes map, where a key is a alias for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordsQueryRes<RecordAtts> query(RecordsQuery query, Map<String, String> attributes, boolean rawAtts);

    @NotNull
    RecordsQueryRes<List<RecordAtts>> query(List<DataValue> foreach, RecordsQuery query, Map<String, String> attributes);

    @NotNull
    RecordsQueryRes<List<RecordAtts>> query(List<DataValue> foreach, RecordsQuery query, Map<String, String> attributes,
                                            boolean rawAtts);

    /* ATTRIBUTES */

    /**
     * The same as getAttribute
     *
     * @return flat record attribute value
     */
    @NotNull
    DataValue getAtt(RecordRef record, String attribute);

    /**
     * Get record attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordAtts getAtts(RecordRef record, Collection<String> attributes);

    /**
     * Get record attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordAtts getAtts(RecordRef record, Collection<String> attributes, boolean rawAtts);

    /**
     * Get record attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordAtts getAtts(RecordRef record, Map<String, String> attributes);

    /**
     * Get record attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordAtts getAtts(RecordRef record, Map<String, String> attributes, boolean rawAtts);

    /**
     * Get records attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    List<RecordAtts> getAtts(Collection<RecordRef> records, Collection<String> attributes);

    /**
     * Get records attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    List<RecordAtts> getAtts(Collection<RecordRef> records, Collection<String> attributes, boolean rawAtts);

    /**
     * Get records attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    List<RecordAtts> getAtts(Collection<RecordRef> records, Map<String, String> attributes);

    /**
     * Get records attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    List<RecordAtts> getAtts(Collection<RecordRef> records, Map<String, String> attributes, boolean rawAtts);

    /* META */

    /**
     * Get record metadata. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified metaClass will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param attsClass DTO to generate metadata schema and retrieve data
     *
     * @see MetaAtt
     */
    @NotNull
    <T> T getAtts(@NotNull RecordRef record, @NotNull Class<T> attsClass);

    /**
     * Get records metadata. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified metaClass will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param attsClass DTO to generate metadata schema and retrieve data
     *
     * @see MetaAtt
     */
    @NotNull
    <T> List<T> getAtts(@NotNull Collection<RecordRef> records, @NotNull Class<T> attsClass);

    /* MUTATION */

    /**
     * Create or change records.
     */
    @NotNull
    RecordRef mutate(RecordAtts meta);

    @NotNull
    List<RecordRef> mutate(List<RecordAtts> records);

    /**
     * Create or change records.
     */
    @NotNull
    RecordRef mutate(RecordRef record, ObjectData attributes);

    /**
     * Create or change records.
     */
    @NotNull
    RecordRef mutate(RecordRef record, Map<String, Object> attributes);

    /**
     * Create or change records.
     */
    @NotNull
    RecordRef mutate(RecordRef record, String attribute, Object value);

    /* DELETION */

    /**
     * Delete records.
     */
    @NotNull
    List<DelStatus> delete(List<RecordRef> records);

    @NotNull
    DelStatus delete(RecordRef recordRef);

    /* OTHER */

    /**
     * Register the RecordsDao. It must return valid id from method "getId()" to call this method.
     */
    void register(RecordsDao recordsSource);

    /**
     * Register the RecordsDao with specified sourceId.
     */
    void register(String sourceId, RecordsDao recordsSource);

    /**
     * Add info about RecordsDao with specified Id.
     */
    @Nullable
    RecsSourceInfo getSourceInfo(String sourceId);

    /**
     * Get info about all registered RecordsDao.
     */
    @NotNull
    List<RecsSourceInfo> getSourcesInfo();
}
