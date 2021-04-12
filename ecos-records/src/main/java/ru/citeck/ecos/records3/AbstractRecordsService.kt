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
import java.util.*

abstract class AbstractRecordsService : RecordsService {

    companion object {
        private val log = KotlinLogging.logger {}
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
        if (record == null || StringUtils.isBlank(attribute)) {
            return DataValue.NULL
        }
        val meta: List<RecordAtts> = getAtts(listOf<Any?>(record), listOf(attribute))
        return meta[0].getAtt(attribute)
    }

    override fun <T : Any> getAtts(record: Any?, attributes: Class<T>): T {
        return getAtts(listOf(record ?: ObjectData.create()), attributes)[0]
    }

    override fun getAtts(records: Collection<*>, attributes: Collection<String>): List<RecordAtts> {
        return getAtts(records, AttUtils.toMap(attributes), false)
    }

    override fun getAtts(record: Any?, attributes: Collection<String>): RecordAtts {
        return getAtts(listOf(record ?: ObjectData.create()), attributes)[0]
    }

    override fun getAtts(record: Any?, attributes: Map<String, *>): RecordAtts {
        return getAtts(listOf(record ?: ObjectData.create()), attributes, false)[0]
    }

    override fun getAtts(records: Collection<*>, attributes: Map<String, *>): List<RecordAtts> {
        return getAtts(records, attributes, false)
    }

    /* MUTATE */

    override fun create(sourceId: String, attributes: Any): RecordRef {
        return mutate(RecordRef.valueOf(sourceId + RecordRef.SOURCE_DELIMITER), attributes)
    }

    override fun mutate(record: Any, attribute: String, value: Any?): RecordRef {
        return if (StringUtils.isBlank(attribute)) {
            if (record is RecordRef) {
                record
            } else {
                error("Mutation of custom objects is not supported yet")
            }
        } else {
            mutate(record, Collections.singletonMap(attribute, value))
        }
    }

    override fun mutate(record: Any, attributes: Map<String, *>): RecordRef {
        return mutate(record, ObjectData.create(attributes))
    }

    override fun mutate(record: Any, attributes: Any): RecordRef {
        return mutate(record, ObjectData.create(attributes))
    }

    override fun mutate(record: Any, attributes: ObjectData): RecordRef {
        if (record is RecordRef) {
            return mutate(RecordAtts(record, attributes))
        }
        error("Mutation of custom objects is not supported yet")
    }

    override fun mutate(record: RecordAtts): RecordRef {
        val records: List<RecordAtts> = listOf(record)
        val recordRefs: List<RecordRef> = this.mutate(records)
        if (recordRefs.size != 1) {
            log.warn("Unexpected result. Expected 1 record, but found " + recordRefs.size)
        }
        return recordRefs[0]
    }

    /* DELETE */
    override fun delete(record: RecordRef): DelStatus {
        val result: List<DelStatus> = delete(listOf(record))
        if (result.size != 1) {
            log.warn("Unexpected result. Expected 1 record, but found " + result.size)
        }
        return result[0]
    }
}
