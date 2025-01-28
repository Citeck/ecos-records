package ru.citeck.ecos.records3.rest.v1.txn

import ru.citeck.ecos.records3.rest.v1.RequestBody
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

class TxnBody : RequestBody() {

    var records: MutableList<EntityRef> = ArrayList()
        private set
    lateinit var action: TxnAction
        private set

    fun setRecords(records: List<EntityRef>?) {
        if (records != null) {
            this.records = ArrayList(records)
        } else {
            this.records = ArrayList()
        }
    }

    fun setAction(action: TxnAction) {
        this.action = action
    }

    enum class TxnAction {
        COMMIT, ROLLBACK
    }
}
