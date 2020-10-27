package ru.citeck.ecos.records3;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.RecordsDao;
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
 * Recs == Records
 *
 * @see AttValue
 * @see RecordRef
 * @see RecordAtts
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
    @Nullable
    RecordRef queryOne(@NotNull RecordsQuery query);

    /**
     * Query single record.
     *
     * @see RecordsService#query(RecordsQuery, Class)
     */
    @Nullable
    <T> T queryOne(@NotNull RecordsQuery query, @NotNull Class<T> metaClass);

    /**
     * Query single record.
     *
     * @see RecordsService#query(RecordsQuery, Collection)
     */
    @Nullable
    RecordAtts queryOne(@NotNull RecordsQuery query, @NotNull Collection<String> attributes);

    @NotNull
    DataValue queryOne(@NotNull RecordsQuery query, @NotNull String attribute);

    /**
     * Query single record.
     *
     * @see RecordsService#query(RecordsQuery, Map)
     */
    @Nullable
    RecordAtts queryOne(@NotNull RecordsQuery query, @NotNull Map<String, ?> attributes);

    /* QUERY RECORDS */

    /**
     * Query records.
     *
     * @return list of records, page info and debug info if query.isDebug() returns true
     */
    @NotNull
    RecsQueryRes<RecordRef> query(@NotNull RecordsQuery query);

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
     * @see AttName
     */
    @NotNull
    <T> RecsQueryRes<T> query(@NotNull RecordsQuery query, @NotNull Class<T> metaClass);

    /**
     * Query records and its attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecsQueryRes<RecordAtts> query(@NotNull RecordsQuery query, @NotNull Collection<String> attributes);

    /**
     * Query records and its attributes.
     *
     * @param attributes map, where a key is a alias for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecsQueryRes<RecordAtts> query(@NotNull RecordsQuery query, @NotNull Map<String, ?> attributes);

    /**
     * Query records and its attributes.
     *
     * @param attributes map, where a key is a alias for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                   @NotNull Map<String, ?> attributes,
                                   boolean rawAtts);

    /* ATTRIBUTES */

    /**
     * The same as getAttribute
     *
     * @return flat record attribute value
     */
    @NotNull
    DataValue getAtt(@Nullable Object record, @NotNull String attribute);

    /**
     * Get record attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordAtts getAtts(@Nullable Object record, @NotNull Collection<String> attributes);

    /**
     * Get record attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    RecordAtts getAtts(@Nullable Object record, @NotNull Map<String, ?> attributes);

    /**
     * Get records attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    List<RecordAtts> getAtts(@NotNull Collection<?> records, @NotNull Collection<String> attributes);

    /**
     * Get records attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    List<RecordAtts> getAtts(@NotNull Collection<?> records, @NotNull Map<String, ?> attributes);

    /**
     * Get record metadata. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified metaClass will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param attsClass DTO to generate metadata schema and retrieve data
     *
     * @see AttName
     */
    @NotNull
    <T> T getAtts(@Nullable Object record, @NotNull Class<T> attsClass);

    /**
     * Get records metadata. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified metaClass will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param attsClass DTO to generate metadata schema and retrieve data
     *
     * @see AttName
     */
    @NotNull
    <T> List<T> getAtts(@NotNull Collection<?> records, @NotNull Class<T> attsClass);

    /**
     * Get records attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     *                   and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    @NotNull
    List<RecordAtts> getAtts(@NotNull Collection<?> records, @NotNull Map<String, ?> attributes, boolean rawAtts);

    /* MUTATE */

    /**
     * Create or change records.
     */
    @NotNull
    RecordRef mutate(@NotNull RecordAtts meta);

    @NotNull
    List<RecordRef> mutate(@NotNull List<RecordAtts> records);

    /**
     * Create or change records.
     */
    @NotNull
    RecordRef mutate(@NotNull Object record, @NotNull ObjectData attributes);

    /**
     * Create or change records.
     */
    @NotNull
    RecordRef mutate(@NotNull Object record, @NotNull Map<String, ?> attributes);

    /**
     * Create or change records.
     */
    @NotNull
    RecordRef mutate(@NotNull Object record, @NotNull String attribute, @Nullable Object value);

    /* DELETE */

    /**
     * Delete records.
     */
    @NotNull
    List<DelStatus> delete(@NotNull List<RecordRef> records);

    @NotNull
    DelStatus delete(@NotNull RecordRef recordRef);

    /* OTHER */

    /**
     * Register the RecordsDao. It must return valid id from method "getId()" to call this method.
     */
    void register(@NotNull RecordsDao recordsSource);

    /**
     * Register the RecordsDao with specified sourceId.
     */
    void register(@NotNull String sourceId, @NotNull RecordsDao recordsSource);

    /**
     * Add info about RecordsDao with specified Id.
     */
    @Nullable
    RecordsDaoInfo getSourceInfo(@NotNull String sourceId);

    /**
     * Get info about all registered RecordsDao.
     */
    @NotNull
    List<RecordsDaoInfo> getSourcesInfo();
}
