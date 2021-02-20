package ru.citeck.ecos.records3.rest.v1.delete

import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.rest.v1.RequestResp
import java.util.*

class DeleteResp : RequestResp() {

    var statuses: MutableList<DelStatus> = ArrayList<DelStatus>()
        private set

    fun setStatuses(statuses: List<DelStatus>?) {
        if (statuses != null) {
            this.statuses = ArrayList(statuses)
        } else {
            this.statuses = ArrayList()
        }
    }
}
