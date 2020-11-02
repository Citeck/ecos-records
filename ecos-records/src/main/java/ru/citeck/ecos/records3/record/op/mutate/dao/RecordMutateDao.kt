package ru.citeck.ecos.records3.record.op.mutate.dao

import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.atts.dto.LocalRecordAtts

interface RecordMutateDao : RecordsDao {

    fun mutate(record: LocalRecordAtts): String
}
