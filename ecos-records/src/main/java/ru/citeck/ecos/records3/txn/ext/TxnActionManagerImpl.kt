package ru.citeck.ecos.records3.txn.ext

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.ReflectUtils
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.concurrent.ConcurrentHashMap

class TxnActionManagerImpl : TxnActionManager {

    companion object {
        private const val TXN_ACTIONS_KEY = "__ext_txn_actions__"
    }

    private val executors: MutableMap<String, ExecutorData> = ConcurrentHashMap()

    override fun execute(action: TxnAction, context: RequestContext?) {
        execute(listOf(action), context)
    }

    override fun execute(entities: List<TxnAction>, context: RequestContext?) {
        executeRaw(entities.map { RawTxnAction(it.type, it.data) }, context)
    }

    override fun execute(type: String, data: Any, context: RequestContext?) {
        executeRaw(listOf(RawTxnAction(type, data)), context)
    }

    override fun executeRaw(actions: List<RawTxnAction>, context: RequestContext?) {

        val txnId = context?.ctxData?.txnId

        if (context == null || txnId == null || context.ctxData.txnOwner) {
            for (action in actions) {
                executeActionByHandler(action.type, action.data)
            }
        } else {
            val ctxList = context.getList<TxnAction>(TXN_ACTIONS_KEY)
            for (action in actions) {
                ctxList.add(TxnAction(action.type, DataValue.create(action.data)))
            }
        }
    }

    override fun register(executor: TxnActionExecutor<*>) {
        val dataType = ReflectUtils.getGenericArg(executor::class.java, TxnActionExecutor::class.java) as Class<*>
        @Suppress("UNCHECKED_CAST")
        val handlerForAny = executor as TxnActionExecutor<Any>
        executors[executor.getType()] = ExecutorData(dataType, handlerForAny)
    }

    override fun getTxnActions(): List<TxnAction> {
        return RequestContext.getCurrent()?.getList(TXN_ACTIONS_KEY) ?: emptyList()
    }

    private fun executeActionByHandler(type: String, entity: Any) {
        val executorData = getExecutorNotNull(type)
        val convertedAction = if (executorData.actionType.isInstance(entity)) {
            entity
        } else {
            Json.mapper.convert(entity, executorData.actionType) ?: error("Entity conversion error. ")
        }
        executorData.executor.execute(convertedAction)
    }

    private fun getExecutorNotNull(type: String): ExecutorData {
        return executors[type] ?: error("Handler with type $type is not found")
    }

    private class ExecutorData(
        val actionType: Class<*>,
        val executor: TxnActionExecutor<Any>
    )
}
