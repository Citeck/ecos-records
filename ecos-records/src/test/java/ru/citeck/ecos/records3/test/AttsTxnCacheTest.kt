package ru.citeck.ecos.records3.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.EcosWebAppApi

class AttsTxnCacheTest : TxnCacheTestBase() {

    private fun newSetup(): Pair<RecordsServiceFactory, CountingDao> {
        val factory = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi = webAppApi
        }
        val dao = CountingDao()
        factory.recordsService.register(dao)
        return factory to dao
    }

    @Test
    fun sameRefHitsDaoOnceInReadOnlyTxn() {
        val (factory, dao) = newSetup()
        val ref = "test@rec-1"
        TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.getAtt(ref, "name")
            factory.recordsService.getAtt(ref, "name")
        }
        assertThat(dao.getAttsCalls).isEqualTo(1)
    }

    @Test
    fun differentRunAsUsersHaveSeparateCacheEntries() {
        val (factory, dao) = newSetup()
        val ref = "test@rec-2"
        TxnContext.doInTxn(readOnly = true) {
            AuthContext.runAs("alice") {
                factory.recordsService.getAtt(ref, "name")
            }
            AuthContext.runAs("bob") {
                factory.recordsService.getAtt(ref, "name")
            }
        }
        assertThat(dao.getAttsCalls).isEqualTo(2)
    }

    @Test
    fun runAsSystemAndRunAsUserHaveSeparateCacheEntries() {
        val (factory, dao) = newSetup()
        val ref = "test@rec-3"
        TxnContext.doInTxn(readOnly = true) {
            AuthContext.runAs("alice") {
                factory.recordsService.getAtt(ref, "name")
            }
            AuthContext.runAsSystem {
                factory.recordsService.getAtt(ref, "name")
            }
        }
        assertThat(dao.getAttsCalls).isEqualTo(2)
    }

    @Test
    fun nonReadOnlyTxnBypassesCache() {
        val (factory, dao) = newSetup()
        val ref = "test@rec-4"
        TxnContext.doInTxn(readOnly = false) {
            factory.recordsService.getAtt(ref, "name")
            factory.recordsService.getAtt(ref, "name")
        }
        assertThat(dao.getAttsCalls).isEqualTo(2)
    }

    @Test
    fun cacheScopeIsBoundedByTransaction() {
        val (factory, dao) = newSetup()
        val ref = "test@rec-5"
        TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.getAtt(ref, "name")
        }
        TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.getAtt(ref, "name")
        }
        assertThat(dao.getAttsCalls).isEqualTo(2)
    }

    private class CountingDao : RecordsAttsDao {

        var getAttsCalls = 0

        override fun getId(): String = "test"

        override fun getRecordsAtts(recordIds: List<String>): List<*> {
            getAttsCalls++
            return recordIds.map { mapOf("name" to "name-$it") }
        }
    }
}
