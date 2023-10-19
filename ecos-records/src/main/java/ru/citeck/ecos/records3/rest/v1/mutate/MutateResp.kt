package ru.citeck.ecos.records3.rest.v1.mutate

import lombok.Getter
import lombok.Setter
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.rest.v1.RequestResp
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

@Getter
@Setter
class MutateResp : RequestResp() {

    var records: MutableList<RecordAtts> = ArrayList()
        private set

    var txnChangedRecords: Set<EntityRef> = emptySet()

    fun setRecords(records: List<RecordAtts>?) {
        if (records != null) {
            this.records = ArrayList(records)
        } else {
            this.records = ArrayList()
        }
    }

    fun addRecord(meta: RecordAtts) {
        records.add(meta)
    }
}
