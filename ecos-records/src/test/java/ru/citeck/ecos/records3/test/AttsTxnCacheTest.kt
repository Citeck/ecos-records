package ru.citeck.ecos.records3.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.RecordAttValueCtx
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

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
    fun attValueCtxWrappingRealRefIsRoutedThroughDaoBatch() {
        val (factory, dao) = newSetup()
        val ref = EntityRef.valueOf("test@rec-real")
        val ctx = RecordAttValueCtx(ref, factory.recordsService)
        TxnContext.doInTxn(readOnly = true) {
            // Both records are in the same getAtts call: extractEntityRefOrKeep
            // must unwrap ctx → ref so they group into a single recordRefs batch.
            // Without unwrap, ctx would land in recordObjs and trigger a recursive
            // getAtt on the bare ref — that's a separate DAO call.
            factory.recordsService.getAtts(listOf(ref, ctx), listOf("name"))
        }
        assertThat(dao.getAttsCalls).isEqualTo(1)
    }

    @Test
    fun attValueCtxWrappingVirtualAttValuePreservesCustomLogic() {
        val (factory, _) = newSetup()
        val customValue = object : AttValue {
            override fun getAtt(name: String): Any? = if (name == "field") "custom" else null
        }
        val ctx = RecordAttValueCtx(customValue, factory.recordsService)
        val result = TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.getAtt(ctx, "field").asText()
        }
        assertThat(result).isEqualTo("custom")
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
