package ru.citeck.ecos.records3

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery

/**
 * Service to work with some abstract "records" from any source.
 * It may be database records, generated data and so on.
 * Each record can be identified by its RecordRef. <br></br><br></br>
 *
 *
 * There are three main purposes: <br></br>
 * 1) Query records by some language, query and other parameters; <br></br>
 * 2) Get records metadata. The metadata in this context is any record data (e.g. document status or title); <br></br>
 * 3) Delete, create or modify records. <br></br><br></br>
 *
 *
 * There are three levels of abstraction for retrieving metadata: <br></br>
 * **DTO Class > Attributes > Schema** <br></br>
 *
 *
 * Each level converts to a schema and sends to RecordsMetaDao or RecordsQueryWithMetaDao.
 * RecordsMetaService can be used to retrieve data by schema from MetaValue or any other java objects. <br></br><br></br>
 *
 *
 * A result of each method to request attributes converts data to a flat view.
 * It means that {"a":{"b":{"c":"value"}}} will be replaced with {"a": "value"}
 *
 * Res == Result
 * Att == Attribute
 * Atts == Attributes
 * Rec == Record
 * Recs == Records
 *
 * @see AttValue
 *
 * @see RecordRef
 *
 * @see RecordAtts
 *
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
     * Create or change records.
     */
    fun mutate(record: RecordAtts): RecordRef

    fun mutate(records: List<RecordAtts>): List<RecordRef>

    /**
     * Create or change records.
     */
    fun mutate(record: Any, attributes: ObjectData): RecordRef

    /**
     * Create or change records.
     */
    fun mutate(record: Any, attributes: Map<String, *>): RecordRef

    /**
     * Create or change records.
     */
    fun mutate(record: Any, attribute: String, value: Any?): RecordRef

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

    /**
     * Add info about RecordsDao with specified Id.
     */
    fun getSourceInfo(sourceId: String): RecordsDaoInfo?

    fun getSourcesInfo(): List<RecordsDaoInfo>
}