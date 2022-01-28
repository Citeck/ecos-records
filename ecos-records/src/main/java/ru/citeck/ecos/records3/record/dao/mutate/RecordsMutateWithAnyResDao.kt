package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.RecordsDao

interface RecordsMutateWithAnyResDao : RecordsDao {

    fun mutateForAnyRes(records: List<LocalRecordAtts>): List<Any>
}
