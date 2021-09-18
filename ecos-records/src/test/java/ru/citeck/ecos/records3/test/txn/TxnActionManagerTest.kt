package ru.citeck.ecos.records3.test.txn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.txn.ext.TxnActionComponent

class TxnActionManagerTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val executedActions = mutableListOf<TxnAction>()
        services.txnActionManager.register(object : TxnActionComponent<TxnAction> {
            override fun preProcess(actions: List<TxnAction>, fromRemote: Boolean): List<TxnAction> {
                return actions
            }
            override fun execute(action: TxnAction) {
                executedActions.add(action)
            }
            override fun getType() = "test"
        })

        val action = TxnAction("test-action-data")
        services.txnActionManager.execute("test", action, RequestContext.getCurrent())
        assertThat(executedActions).containsExactly(action)
        executedActions.clear()

        RequestContext.doWithTxn {
            services.txnActionManager.execute("test", action, RequestContext.getCurrent())
            assertThat(executedActions).containsExactly(action)
            executedActions.clear()
        }

        val otherAction = OtherTxnActionWithSameData("abc")
        services.txnActionManager.execute("test", otherAction, RequestContext.getCurrent())
        assertThat(executedActions).containsExactly(TxnAction(otherAction.field))
    }

    data class TxnAction(
        val field: String
    )

    data class OtherTxnActionWithSameData(
        val field: String
    )
}
