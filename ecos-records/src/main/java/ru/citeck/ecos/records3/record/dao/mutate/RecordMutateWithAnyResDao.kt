package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.RecordsDao
import kotlin.jvm.Throws

interface RecordMutateWithAnyResDao : RecordsDao {

    @Throws(Exception::class)
    fun mutateForAnyRes(record: LocalRecordAtts): Any?
}
