package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordsMutateDao : RecordsDao {

    @Throws(Exception::class)
    fun mutate(records: List<LocalRecordAtts>): List<String>
}
