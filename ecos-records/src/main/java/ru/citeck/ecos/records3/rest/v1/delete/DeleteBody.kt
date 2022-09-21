package ru.citeck.ecos.records3.rest.v1.delete

import ru.citeck.ecos.records3.rest.v1.RequestBody
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

class DeleteBody : RequestBody() {

    var records: MutableList<EntityRef> = ArrayList<EntityRef>()
        private set

    fun setRecords(records: List<EntityRef>?) {
        if (records != null) {
            this.records = ArrayList<EntityRef>(records)
        } else {
            this.records = ArrayList()
        }
    }

    fun setRecord(record: EntityRef) {
        records.add(record)
    }
}
