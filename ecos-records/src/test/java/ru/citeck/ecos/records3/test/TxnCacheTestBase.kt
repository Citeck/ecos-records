package ru.citeck.ecos.records3.test

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.txn.lib.manager.TransactionManagerImpl

/**
 * Boots a real TransactionManagerImpl (no mocks) and registers it with TxnContext
 * for tests that exercise transaction-scoped behavior in the records resolver.
 *
 * `TxnContext.setManager` writes to a global singleton; subclasses share the same
 * static slot, so all tests see one manager — that is the intended behavior here.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class TxnCacheTestBase {

    protected lateinit var webAppApi: EcosWebAppApiMock
    protected lateinit var txnManager: TransactionManagerImpl

    @BeforeAll
    fun setUpTxnManager() {
        webAppApi = EcosWebAppApiMock()
        txnManager = TransactionManagerImpl()
        txnManager.init(webAppApi)
        TxnContext.setManager(txnManager)
    }

    @AfterAll
    fun tearDownTxnManager() {
        txnManager.shutdown()
        webAppApi.dispose()
    }
}
