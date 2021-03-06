package ru.citeck.ecos.records3.rest.v1

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.request.msg.ReqMsg
import ru.citeck.ecos.records3.txn.ext.TxnAction
import kotlin.collections.ArrayList

abstract class RequestResp {

    var messages: MutableList<ReqMsg> = ArrayList()
        private set

    var txnActions: List<TxnAction> = emptyList()
        private set

    fun setMessages(messages: List<ReqMsg>?) {
        if (messages != null) {
            this.messages = ArrayList(messages)
        } else {
            this.messages = ArrayList()
        }
    }

    fun setTxnActions(txnActions: List<TxnAction>?) {
        if (txnActions != null) {
            this.txnActions = ArrayList(txnActions)
        } else {
            this.txnActions = ArrayList()
        }
    }

    val version: Int
        get() = 1

    override fun toString(): String {
        return Json.mapper.toString(this) ?: "RequestResp"
    }
}
