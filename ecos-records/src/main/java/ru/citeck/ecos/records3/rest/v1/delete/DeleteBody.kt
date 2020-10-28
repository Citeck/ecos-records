package ru.citeck.ecos.records3.rest.v1.delete

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.rest.v1.RequestBody
import java.util.*

class DeleteBody : RequestBody() {

    var records: MutableList<RecordRef> = ArrayList<RecordRef>()
        private set

    fun setRecords(records: List<RecordRef>?) {
        if (records != null) {
            this.records = ArrayList<RecordRef>(records)
        } else {
            this.records = ArrayList()
        }
    }

    fun setRecord(record: RecordRef) {
        records.add(record)
    }
}
