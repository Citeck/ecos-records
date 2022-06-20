package ru.citeck.ecos.records3.record.dao.impl.mem

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.SupportsQueryLanguages
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Records DAO with simple in-memory storage system based on Map<String, ObjectData>
 */
open class InMemDataRecordsDao(
    private val id: String
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordMutateDao,
    RecordDeleteDao,
    RecordsQueryDao,
    SupportsQueryLanguages {

    companion object {
        private val SUPPORTED_LANGUAGES = listOf("", PredicateService.LANGUAGE_PREDICATE)
    }

    private val records = ConcurrentHashMap<String, ObjectData>()

    override fun getId() = id

    override fun getRecordAtts(recordId: String): Any? {
        return records[recordId]
    }

    override fun delete(recordId: String): DelStatus {
        records.remove(recordId)
        return DelStatus.OK
    }

    override fun mutate(record: LocalRecordAtts): String {

        val customId = record.attributes.get("id", "")
        if (customId.isNotEmpty() && record.id != customId && records.containsKey(customId)) {
            error("Record with id '$customId' already exists")
        }

        return if (record.id.isEmpty()) {
            val newId = customId.ifEmpty { UUID.randomUUID().toString() }
            records[newId] = record.attributes
            newId
        } else {
            val mutRec = records[record.id]?.deepCopy() ?: error("Record is not found with id: ${record.id}")
            record.attributes.forEach { k, v ->
                mutRec.set(k, v)
            }
            records[customId.ifEmpty { record.id }] = mutRec
            record.id
        }
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any {
        val recordsList = records.entries.map { Record(it.key, it.value) }
        if (recsQuery.language == "") {
            return recordsList
        }
        if (recsQuery.language != PredicateService.LANGUAGE_PREDICATE) {
            error("Unsupported query language: ${recsQuery.language}")
        }
        val predicate = recsQuery.getQuery(Predicate::class.java)

        return predicateService.filterAndSort(
            recordsList,
            predicate,
            recsQuery.sortBy,
            recsQuery.page.skipCount,
            recsQuery.page.maxItems
        )
    }

    fun getRecords(): Map<String, ObjectData> {
        return records
    }

    override fun getSupportedLanguages(): List<String> {
        return SUPPORTED_LANGUAGES
    }

    private class Record(val id: String, val atts: ObjectData) : AttValue {

        override fun getId(): Any {
            return id
        }

        override fun getAtt(name: String): Any {
            return atts.get(name)
        }
    }
}
