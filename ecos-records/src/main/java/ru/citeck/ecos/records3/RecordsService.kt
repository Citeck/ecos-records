package ru.citeck.ecos.records3

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateCrossSrcDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

/**
 * Service to work with some abstract "records" from any source.
 * It may be database records, generated data and so on.
 * Each record can be identified by its RecordRef. <br></br><br></br>
 *
 * There are four main purposes: <br></br>
 * 1) Query records by some language, query and other parameters; <br></br>
 * 2) Get records attributes. The attributes in this context is any record data (e.g. document status or title); <br></br>
 * 3) Mutate (create or modify) records. <br></br>
 * 4) Delete records. <br></br><br></br>
 *
 * There are two levels of abstraction for retrieving metadata: <br></br>
 * **DTO Class > Attributes** <br></br>
 *
 * Each method to get attributes converts data to a flat view if flag "rawData" is not set.
 * It means that {"a":{"b":{"c":"value"}}} will be replaced with {"a": "value"}<br></br><br></br>
 *
 * Terms:<br></br>
 * Res == Result<br></br>
 * Att == Attribute<br></br>
 * Atts == Attributes<br></br>
 * Rec == Record<br></br>
 * Recs == Records<br></br>
 *
 * @see RecordAttsDao
 * @see RecordsAttsDao
 *
 * @see RecordMutateDao
 * @see RecordsMutateDao
 * @see RecordsMutateCrossSrcDao
 * @see RecordMutateDtoDao
 *
 * @see RecordsQueryDao
 *
 * @see RecordDeleteDao
 * @see RecordsDeleteDao
 *
 * @see AttValue
 * @see RecordRef
 * @see RecordAtts
 *
 * @author Pavel Simonov
 */
interface RecordsService {

    /* QUERY RECORD */

    /**
     * Query single record.
     *
     * @see RecordsService.query
     */
    fun queryOne(query: RecordsQuery): RecordRef?

    /**
     * Query single record.
     *
     * @see RecordsService.query
     */
    fun <T : Any> queryOne(query: RecordsQuery, attributes: Class<T>): T?

    /**
     * Query single record.
     *
     * @see RecordsService.query
     */
    fun queryOne(query: RecordsQuery, attributes: Collection<String>): RecordAtts?

    /**
     * Query single record.
     *
     * @see RecordsService.query
     */
    fun queryOne(query: RecordsQuery, attributes: Map<String, *>): RecordAtts?

    fun queryOne(query: RecordsQuery, attribute: String): DataValue

    /* QUERY RECORDS */

    /**
     * Query records.
     *
     * @return list of records, page info and debug info if query.isDebug() returns true
     */
    fun query(query: RecordsQuery): RecsQueryRes<RecordRef>

    /**
     * Query records with data. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified attributes will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param query query to search records
     * @param attributes DTO to generate metadata schema and retrieve data
     *
     * @see AttName
     */
    fun <T : Any> query(query: RecordsQuery, attributes: Class<T>): RecsQueryRes<T>

    /**
     * Query records and its attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    fun query(query: RecordsQuery, attributes: Collection<String>): RecsQueryRes<RecordAtts>

    /**
     * Query records and its attributes.
     *
     * @param attributes map, where a key is a alias for a result value
     * and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    fun query(query: RecordsQuery, attributes: Map<String, *>): RecsQueryRes<RecordAtts>

    /**
     * Query records and its attributes.
     *
     * @param attributes map, where a key is a alias for a result value
     * and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    fun query(query: RecordsQuery, attributes: Map<String, *>, rawAtts: Boolean): RecsQueryRes<RecordAtts>

    /* ATTRIBUTES */

    /**
     * The same as getAttribute
     *
     * @return flat record attribute value
     */
    fun getAtt(record: Any?, attribute: String): DataValue

    /**
     * Get record attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    fun getAtts(record: Any?, attributes: Collection<String>): RecordAtts

    /**
     * Get record attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     * and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    fun getAtts(record: Any?, attributes: Map<String, *>): RecordAtts

    /**
     * Get records attributes.
     *
     * @param attributes collection of attributes which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    fun getAtts(records: Collection<*>, attributes: Collection<String>): List<RecordAtts>

    /**
     * Get record metadata. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified attributes will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param attributes DTO to generate metadata schema and retrieve data
     *
     * @see AttName
     */
    fun <T : Any> getAtts(record: Any?, attributes: Class<T>): T

    /**
     * Get records metadata. Specified class will be used to determine
     * which attributes will be requested from a data source. Each property
     * with setter from the specified attributes will be interpreted as an
     * attribute for the request.
     * You can use MetaAtt annotation to change the requested attribute.
     *
     * @param attributes DTO to generate metadata schema and retrieve data
     *
     * @see AttName
     */
    fun <T : Any> getAtts(records: Collection<*>, attributes: Class<T>): List<T>

    /**
     * Get records attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     * and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    fun getAtts(records: Collection<*>, attributes: Map<String, *>): List<RecordAtts>

    /**
     * Get records attributes.
     *
     * @param attributes map, where a key is a pseudonym for a result value
     * and value is an attribute which value we want to request.
     *
     * @return flat records metadata (all objects in attributes with a single key will be simplified)
     */
    fun getAtts(records: Collection<*>, attributes: Map<String, *>, rawAtts: Boolean): List<RecordAtts>

    /* MUTATE */

    /**
     * Same as mutate but for '{{sourceId}}@' record (RecordRef without local ID)
     */
    fun create(sourceId: String, attributes: Any): RecordRef

    /**
     * Create or change record.
     */
    fun mutate(record: RecordAtts): RecordRef

    /**
     * Create or change record and load attributes from result
     */
    fun mutate(record: RecordAtts, attsToLoad: Map<String, *>): RecordAtts

    /**
     * Create or change records.
     */
    fun mutate(records: List<RecordAtts>): List<RecordRef>

    /**
     * Create or change records and load attributes from result.
     */
    fun mutate(records: List<RecordAtts>, attsToLoad: Map<String, *>, rawAtts: Boolean): List<RecordAtts>

    /**
     * Create or change record.
     */
    fun mutate(record: Any, attributes: ObjectData): RecordRef

    /**
     * Create or change records and load attributes from result.
     */
    fun mutate(record: Any, attributes: ObjectData, attsToLoad: Map<String, *>): RecordAtts

    /**
     * Create or change record.
     */
    fun mutate(record: Any, attributes: Any): RecordRef

    /**
     * Create or change record and load attributes from result.
     */
    fun mutate(record: Any, attributes: Any, attsToLoad: Map<String, *>): RecordAtts

    /**
     * Create or change record and load attributes from result.
     */
    fun mutate(record: Any, attributes: Any, attsToLoad: Collection<String>): RecordAtts

    /**
     * Create or change record and load attributes from result.
     */
    fun <T : Any> mutate(record: Any, attributes: Any, attsToLoad: Class<T>): T

    /**
     * Create or change records.
     */
    fun mutate(record: Any, attributes: Map<String, *>): RecordRef

    /**
     * Create or change record with single attribute
     */
    fun mutateAtt(record: Any, attribute: String, value: Any?): RecordRef

    /* DELETE */

    /**
     * Delete records.
     */
    fun delete(records: List<RecordRef>): List<DelStatus>

    fun delete(record: RecordRef): DelStatus

    /* OTHER */

    /**
     * Register the RecordsDao. It must return valid id from method "getId()" to call this method.
     */
    fun register(recordsSource: RecordsDao)

    /**
     * Register the RecordsDao with specified sourceId.
     */
    fun register(sourceId: String, recordsSource: RecordsDao)

    fun unregister(sourceId: String)

    fun <T : Any> getRecordsDao(sourceId: String, type: Class<T>): T?
}
