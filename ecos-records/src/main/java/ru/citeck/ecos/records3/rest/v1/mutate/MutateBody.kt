package ru.citeck.ecos.records3.rest.v1.mutate

import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.rest.v1.RequestBody
import java.util.*

class MutateBody : RequestBody() {

    private var records: MutableList<RecordAtts> = ArrayList<RecordAtts>()

    fun setRecord(record: RecordAtts) {
        records.add(record)
    }

    fun getRecords(): List<RecordAtts> {
        return records
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
