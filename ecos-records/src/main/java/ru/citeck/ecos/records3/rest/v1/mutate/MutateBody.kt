package ru.citeck.ecos.records3.rest.v1.mutate

import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.rest.v1.RequestBody
import java.util.*

class MutateBody : RequestBody() {

    var records: MutableList<RecordAtts> = ArrayList<RecordAtts>()
        private set

    fun setRecord(record: RecordAtts) {
        records.add(record)
    }

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
