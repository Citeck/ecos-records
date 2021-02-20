package ru.citeck.ecos.records3.rest.v1.query

import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.rest.v1.RequestResp
import java.util.*

class QueryResp : RequestResp() {

    var records: MutableList<RecordAtts> = ArrayList<RecordAtts>()
        private set

    var hasMore = false
    var totalCount: Long = 0

    fun setRecords(records: List<RecordAtts>?) {
        if (records != null) {
            this.records = ArrayList(records)
        } else {
            this.records = ArrayList()
        }
    }
}
