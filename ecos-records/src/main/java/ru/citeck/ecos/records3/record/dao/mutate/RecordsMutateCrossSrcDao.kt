package ru.citeck.ecos.records3.record.dao.mutate

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.jvm.Throws

@Deprecated(
    "Records can't be mutated as list." +
        "mutate method of this interface always called for every record separately " +
        "and for optimization and reducing of code complexity " +
        "please use RecordMutateWithAnyResDao instead of using this interface"
)
interface RecordsMutateCrossSrcDao : RecordsDao {

    @Throws(Exception::class)
    fun mutate(records: List<LocalRecordAtts>): List<EntityRef>
}
