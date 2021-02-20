package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordMutateDao : RecordsDao {

    fun mutate(record: LocalRecordAtts): String
}
