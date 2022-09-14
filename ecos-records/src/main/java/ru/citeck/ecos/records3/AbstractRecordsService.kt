package ru.citeck.ecos.records3

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.utils.AttUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

abstract class AbstractRecordsService : RecordsService {

    companion object {
        private val log = KotlinLogging.logger {}

        private val EMPTY_ATTS_MAP = emptyMap<String, Any>()
    }

    /* QUERY */

    override fun queryOne(query: RecordsQuery): RecordRef? {
        return query(query).getRecords().firstOrNull()
    }

    override fun <T : Any> queryOne(query: RecordsQuery, attributes: Class<T>): T? {
        return query(query, attributes).getRecords().firstOrNull()
    }

    override fun queryOne(query: RecordsQuery, attributes: Map<String, *>): RecordAtts? {
        return query(query, attributes).getRecords().firstOrNull()
    }

    override fun queryOne(query: RecordsQuery, attributes: Collection<String>): RecordAtts? {
        return query(query, attributes).getRecords().firstOrNull()
    }

    override fun queryOne(query: RecordsQuery, attribute: String): DataValue {
        return query(query, setOf(attribute))
            .getRecords()
            .map { it.getAtt(attribute) }
            .firstOrNull() ?: DataValue.NULL
    }

    override fun query(query: RecordsQuery, attributes: Collection<String>): RecsQueryRes<RecordAtts> {
        return query(query, AttUtils.toMap(attributes))
    }

    override fun query(query: RecordsQuery, attributes: Map<String, *>): RecsQueryRes<RecordAtts> {

        return query(query, attributes, false)
    }

    /* ATTRIBUTES */

    override fun getAtt(record: Any?, attribute: String): DataValue {
        if (StringUtils.isBlank(attribute)) {
            return DataValue.NULL
        }
        val meta: List<RecordAtts> = getAtts(listOf(record), listOf(attribute))
        return meta[0].getAtt(attribute)
    }

    override fun <T : Any> getAtts(record: Any?, attributes: Class<T>): T {
        return getAtts(listOf(record), attributes)[0]
    }

    override fun getAtts(records: Collection<*>, attributes: Collection<String>): List<RecordAtts> {
        return getAtts(records, AttUtils.toMap(attributes), false)
    }

    override fun getAtts(record: Any?, attributes: Collection<String>): RecordAtts {
        return getAtts(listOf(record), attributes)[0]
    }

    override fun getAtts(record: Any?, attributes: Map<String, *>): RecordAtts {
        return getAtts(listOf(record), attributes, false)[0]
    }

    override fun getAtts(records: Collection<*>, attributes: Map<String, *>): List<RecordAtts> {
        return getAtts(records, attributes, false)
    }

    /* MUTATE */

    override fun create(sourceId: String, attributes: Any): RecordRef {
        return mutate(RecordRef.valueOf(sourceId + RecordRef.SOURCE_DELIMITER), attributes)
    }

    override fun mutateAtt(record: Any, attribute: String, value: Any?): RecordRef {
        return mutate(record, Collections.singletonMap(attribute, value))
    }

    override fun mutate(record: Any, attributes: Map<String, *>): RecordRef {
        return mutate(record, ObjectData.create(attributes))
    }

    override fun mutate(record: Any, attributes: Any): RecordRef {
        return mutate(record, ObjectData.create(attributes))
    }

    override fun mutate(record: Any, attributes: ObjectData): RecordRef {
        return mutate(record, attributes, EMPTY_ATTS_MAP).getId()
    }

    override fun mutate(record: Any, attributes: ObjectData, attsToLoad: Map<String, *>): RecordAtts {
        val ref = when (record) {
            is String -> RecordRef.valueOf(record)
            is RecordRef -> record
            else -> error("Mutation of custom objects is not supported yet")
        }
        return mutate(RecordAtts(ref, attributes), attsToLoad)
    }

    override fun mutate(record: RecordAtts): RecordRef {
        return mutate(record, EMPTY_ATTS_MAP).getId()
    }

    override fun mutate(record: RecordAtts, attsToLoad: Map<String, *>): RecordAtts {
        val records: List<RecordAtts> = listOf(record)
        val recordAtts: List<RecordAtts> = this.mutate(records, attsToLoad, false)
        if (recordAtts.size != 1) {
            error("Unexpected result. Expected 1 record, but found ${recordAtts.size}. Ref: ${record.getId()}")
        }
        return recordAtts[0]
    }

    override fun mutate(record: Any, attributes: Any, attsToLoad: Collection<String>): RecordAtts {
        return mutate(record, attributes, AttUtils.toMap(attsToLoad))
    }

    override fun mutate(records: List<RecordAtts>): List<RecordRef> {
        return mutate(records, EMPTY_ATTS_MAP, false).map { it.getId() }
    }

    override fun mutate(record: Any, attributes: Any, attsToLoad: Map<String, *>): RecordAtts {
        return mutate(record, ObjectData.create(attributes), attsToLoad)
    }

    /* DELETE */

    override fun delete(record: String): DelStatus {
        return delete(RecordRef.valueOf(record))
    }

    override fun delete(record: RecordRef): DelStatus {
        return delete(record as EntityRef)
    }

    override fun delete(record: EntityRef): DelStatus {
        val result: List<DelStatus> = delete(listOf(record))
        if (result.size != 1) {
            log.warn("Unexpected result. Expected 1 record, but found " + result.size)
        }
        return result[0]
    }
}
