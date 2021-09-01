package ru.citeck.ecos.records3.iter

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class IterableRecordRefs(
    query: RecordsQuery,
    config: IterableRecordsConfig = IterableRecordsConfig.EMPTY,
    recordsService: RecordsService
) : Iterable<RecordRef> {

    private val iterableRecords = IterableRecords(query, config, recordsService)

    override fun iterator(): RecordsIterator<RecordRef> {
        return Iter(iterableRecords.iterator())
    }

    private class Iter(val impl: RecordsIterator<RecordAtts>) : RecordsIterator<RecordRef> {

        override fun hasNext() = impl.hasNext()
        override fun next() = impl.next().getId()
        override fun getProcessedCount() = impl.getProcessedCount()
        override fun getTotalCount() = impl.getTotalCount()
    }
}
