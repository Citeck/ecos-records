package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.jvm.Throws

interface RecordsMutateCrossSrcDao : RecordsDao {

    @Throws(Exception::class)
    fun mutate(records: List<LocalRecordAtts>): List<EntityRef>
}
