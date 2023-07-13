package ru.citeck.ecos.records3.record.dao.impl.mem

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
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
import ru.citeck.ecos.webapp.api.entity.EntityRef
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
        private val DISPLAY_NAME_ATTS = listOf("_disp", "name")
    }

    private val records = ConcurrentHashMap<String, ObjectData>()

    override fun getId() = id

    override fun getRecordAtts(recordId: String): Any? {
        return records[recordId]?.let { Record(recordId, it) }
    }

    override fun delete(recordId: String): DelStatus {
        records.remove(recordId)
        return DelStatus.OK
    }

    override fun mutate(record: LocalRecordAtts): String {

        val idFromAtts = record.getAtt("id", "")

        val (srcId, targetId) = if (record.id.isNotEmpty()) {
            if (!records.containsKey(record.id)) {
                error("Record with id '${record.id}' is not found!")
            }
            if (idFromAtts.isNotEmpty() && idFromAtts != record.id && records.containsKey(idFromAtts)) {
                error("Record with id $idFromAtts already registered")
            }
            if (record.id == idFromAtts) {
                record.id to ""
            } else {
                record.id to idFromAtts
            }
        } else {
            if (idFromAtts.isNotEmpty()) {
                if (records.containsKey(idFromAtts)) {
                    idFromAtts to ""
                } else {
                    "" to idFromAtts
                }
            } else {
                "" to UUID.randomUUID().toString()
            }
        }
        if (srcId.isEmpty() && targetId.isEmpty()) {
            // should not occur by logic before
            error("Invalid state")
        }

        return if (srcId.isEmpty()) {
            records[targetId] = record.attributes.deepCopy()
            targetId
        } else {
            val mutRec = records[srcId]?.deepCopy() ?: error("Record is not found with id: $srcId")
            record.attributes.forEach { k, v ->
                mutRec[k] = v
            }
            val newId = targetId.ifEmpty { srcId }
            records[newId] = mutRec
            newId
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

        override fun getDisplayName(): Any? {
            for (dispAtt in DISPLAY_NAME_ATTS) {
                val disp = atts[dispAtt]
                if (disp.isNotEmpty()) {
                    return if (disp.isObject()) {
                        disp.getAs(MLText::class.java)
                    } else {
                        disp
                    }
                }
            }
            return null
        }

        override fun getAtt(name: String): Any {
            return atts[name]
        }

        override fun asJson(): Any {
            return atts
        }

        override fun getType(): Any? {
            val typeFromAtts = atts[RecordConstants.ATT_TYPE].asText()
            return if (typeFromAtts.isNotEmpty()) {
                EntityRef.valueOf(typeFromAtts)
            } else {
                null
            }
        }
    }
}
