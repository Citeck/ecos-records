package ru.citeck.ecos.records3.txn.ext

import ru.citeck.ecos.records3.record.request.RequestContext

interface TxnActionManager {

    fun execute(action: TxnAction, context: RequestContext?)

    fun execute(actions: List<TxnAction>, context: RequestContext?)

    fun execute(type: String, data: Any, context: RequestContext?)

    fun executeRaw(actions: List<RawTxnAction>, context: RequestContext?)

    fun register(component: TxnActionComponent<*>)

    fun getTxnActions(context: RequestContext?): List<TxnAction>
}
