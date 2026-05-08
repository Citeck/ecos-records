package ru.citeck.ecos.records3.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.EcosWebAppApi

class QueryTxnCacheTest : TxnCacheTestBase() {

    private fun newTestSetup(): Pair<RecordsServiceFactory, CountingQueryDao> {
        val factory = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi = webAppApi
        }
        val dao = CountingQueryDao()
        factory.recordsService.register(dao)
        return factory to dao
    }

    private fun makeQuery(value: String) = RecordsQuery.create()
        .withSourceId("test-src")
        .withQuery(DataValue.createObj().set("v", value))
        .build()

    @Test
    fun sameQueryHitsDaoOnceInReadOnlyTxn() {
        val (factory, dao) = newTestSetup()
        TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.query(makeQuery("a"), listOf("k"))
            factory.recordsService.query(makeQuery("a"), listOf("k"))
        }
        assertThat(dao.queryCalls).isEqualTo(1)
    }

    @Test
    fun differentQueriesEachHitDao() {
        val (factory, dao) = newTestSetup()
        TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.query(makeQuery("a"), listOf("k"))
            factory.recordsService.query(makeQuery("b"), listOf("k"))
        }
        assertThat(dao.queryCalls).isEqualTo(2)
    }

    @Test
    fun sameQueryInNonReadOnlyTxnIsNotCached() {
        val (factory, dao) = newTestSetup()
        TxnContext.doInTxn(readOnly = false) {
            factory.recordsService.query(makeQuery("a"), listOf("k"))
            factory.recordsService.query(makeQuery("a"), listOf("k"))
        }
        assertThat(dao.queryCalls).isEqualTo(2)
    }

    @Test
    fun cacheScopeIsBoundedByTransaction() {
        val (factory, dao) = newTestSetup()
        TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.query(makeQuery("a"), listOf("k"))
        }
        TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.query(makeQuery("a"), listOf("k"))
        }
        assertThat(dao.queryCalls).isEqualTo(2)
    }

    @Test
    fun differentRunAsUsersHaveSeparateCacheEntries() {
        val (factory, dao) = newTestSetup()
        TxnContext.doInTxn(readOnly = true) {
            AuthContext.runAs("alice") {
                factory.recordsService.query(makeQuery("a"), listOf("k"))
                factory.recordsService.query(makeQuery("a"), listOf("k"))
            }
            AuthContext.runAs("bob") {
                factory.recordsService.query(makeQuery("a"), listOf("k"))
            }
        }
        assertThat(dao.queryCalls).isEqualTo(2)
    }

    @Test
    fun differentAttsToLoadBypassesCache() {
        val (factory, dao) = newTestSetup()
        TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.query(makeQuery("a"), listOf("k"))
            factory.recordsService.query(makeQuery("a"), listOf("k", "extra"))
        }
        assertThat(dao.queryCalls).isEqualTo(2)
    }

    @Test
    fun nestedQueryWithSameKeyDoesNotTriggerRecursiveUpdateOnCache() {
        // Regression: a query whose handler triggers another query for the SAME key (or any
        // key colliding into the same ConcurrentHashMap bin) must not hit the JDK's
        // "Recursive update" guard inside computeIfAbsent. Production stacks show this when
        // a permission cascade nested in the resolver call recurses back into RecordsService.
        // Using the same query+atts is the most direct way to guarantee the bin collision.
        val factory = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi = webAppApi
        }
        val q = RecordsQuery.create().withSourceId("recursive-src").build()
        val callCount = java.util.concurrent.atomic.AtomicInteger()
        val dao = object : RecordsQueryDao {
            override fun getId() = "recursive-src"
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                if (callCount.getAndIncrement() == 0) {
                    // Re-enter the same cached key while the outer call is still in flight.
                    factory.recordsService.query(q, listOf("k"))
                }
                return emptyList<String>()
            }
        }
        factory.recordsService.register(dao)

        TxnContext.doInTxn(readOnly = true) {
            factory.recordsService.query(q, listOf("k"))
        }
        // Reaches here only if no "Recursive update" was thrown from the cache.
    }

    private class CountingQueryDao : RecordsQueryDao, RecordsAttsDao {

        var queryCalls = 0

        override fun getId(): String = "test-src"

        override fun queryRecords(recsQuery: RecordsQuery): Any {
            queryCalls++
            return listOf("rec1", "rec2")
        }

        override fun getRecordsAtts(recordIds: List<String>): List<*> {
            return recordIds.map { mapOf("k" to "value-$it", "extra" to "x-$it") }
        }
    }
}
