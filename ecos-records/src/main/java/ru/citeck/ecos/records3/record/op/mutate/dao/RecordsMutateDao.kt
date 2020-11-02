package ru.citeck.ecos.records3.record.op.mutate.dao

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.atts.dto.LocalRecordAtts

interface RecordsMutateDao : RecordsDao {

    fun mutate(records: List<LocalRecordAtts>): List<String>
}
