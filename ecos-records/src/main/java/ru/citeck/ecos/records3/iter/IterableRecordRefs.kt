package ru.citeck.ecos.records3.iter

import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef

class IterableRecordRefs(
    query: RecordsQuery,
    config: IterableRecordsConfig = IterableRecordsConfig.EMPTY,
    recordsService: RecordsService
) : Iterable<EntityRef> {

    private val iterableRecords = IterableRecords(query, config, recordsService)

    override fun iterator(): RecordsIterator<EntityRef> {
        return Iter(iterableRecords.iterator())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }
        other as IterableRecordRefs
        if (iterableRecords != other.iterableRecords) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        return iterableRecords.hashCode()
    }

    private class Iter(val impl: RecordsIterator<RecordAtts>) : RecordsIterator<EntityRef> {

        override fun hasNext() = impl.hasNext()
        override fun next() = impl.next().getId()
        override fun getProcessedCount() = impl.getProcessedCount()
        override fun getTotalCount() = impl.getTotalCount()
    }
}
