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

    private val components: MutableMap<String, ComponentData> = ConcurrentHashMap()

    override fun preProcess(actions: List<TxnAction>, fromRemote: Boolean): List<TxnAction> {

        if (actions.isEmpty()) {
            return actions
        }

        val actionsByType = linkedMapOf<String, MutableList<TxnAction>>()
        actions.forEach { action -> actionsByType.computeIfAbsent(action.type) { ArrayList() }.add(action) }

        val result = ArrayList<TxnAction>()

        actionsByType.forEach { (type, typeActions) ->

            val componentData = getComponentNotNull(type)
            val convertedActions = mutableListOf<Any>()
            typeActions.forEach {
                convertedActions.add(
                    it.data.getAs(componentData.actionType)
                        ?: error("Action with type $type can't be converted to ${componentData.actionType}")
                )
            }
            val processed = componentData.component.preProcess(convertedActions, fromRemote)

            result.addAll(processed.map { TxnAction(type, DataValue.create(it)) })
        }

        return result
    }

    override fun execute(action: TxnAction, context: RequestContext?) {
        execute(listOf(action), context)
    }

    override fun execute(actions: List<TxnAction>, context: RequestContext?) {
        executeRaw(actions.map { RawTxnAction(it.type, it.data) }, context)
    }

    override fun execute(type: String, data: Any, context: RequestContext?) {
        executeRaw(listOf(RawTxnAction(type, data)), context)
    }

    override fun executeRaw(actions: List<RawTxnAction>, context: RequestContext?) {

        if (actions.isEmpty()) {
            return
        }

        val txnId = context?.ctxData?.txnId

        if (context == null || txnId == null || context.ctxData.txnOwner) {
            for (action in actions) {
                executeActionByComponent(action.type, action.data)
            }
        } else {
            val ctxList = context.getList<TxnAction>(TXN_ACTIONS_KEY)
            for (action in actions) {
                ctxList.add(TxnAction(action.type, DataValue.create(action.data)))
            }
        }
    }

    override fun register(component: TxnActionComponent<*>) {
        val dataType = ReflectUtils.getGenericArg(component::class.java, TxnActionComponent::class.java) as Class<*>
        @Suppress("UNCHECKED_CAST")
        val handlerForAny = component as TxnActionComponent<Any>
        components[component.getType()] = ComponentData(dataType, handlerForAny)
    }

    override fun getTxnActions(context: RequestContext?): List<TxnAction> {
        return context?.getList(TXN_ACTIONS_KEY) ?: emptyList()
    }

    private fun executeActionByComponent(type: String, entity: Any) {
        val componentData = getComponentNotNull(type)
        val convertedAction = if (componentData.actionType.isInstance(entity)) {
            entity
        } else {
            Json.mapper.convert(entity, componentData.actionType) ?: error("Action conversion error")
        }
        componentData.component.execute(convertedAction)
    }

    private fun getComponentNotNull(type: String): ComponentData {
        return components[type] ?: error("Component with type $type is not found")
    }

    private class ComponentData(
        val actionType: Class<*>,
        val component: TxnActionComponent<Any>
    )
}
