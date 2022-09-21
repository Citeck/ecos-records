package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordMutateDao : RecordsDao {

    @Throws(Exception::class)
    fun mutate(record: LocalRecordAtts): String
}
