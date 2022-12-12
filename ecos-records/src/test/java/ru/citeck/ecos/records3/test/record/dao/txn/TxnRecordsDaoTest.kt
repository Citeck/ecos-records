package ru.citeck.ecos.records3.test.record.dao.txn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.promise.Promises
import ru.citeck.ecos.commons.test.EcosWebAppApiMock
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.txn.TxnRecordsDao
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.api.web.EcosWebClientApi
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.concurrent.thread

class TxnRecordsDaoTest {

    @Test
    fun proxyTest() {

        val targetId = "target-id"
        val proxyId = "proxy-id"

        val commitRecs = mutableListOf<String>()
        val rollbackRecs = mutableListOf<String>()

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(object : RecordAttsDao, TxnRecordsDao, RecordDeleteDao, RecordMutateDao {
            override fun getId() = targetId
            override fun getRecordAtts(recordId: String): Any? {
                return null
            }
            override fun commit(txnId: UUID, recordIds: List<String>) {
                commitRecs.addAll(recordIds)
            }
            override fun rollback(txnId: UUID, recordIds: List<String>) {
                rollbackRecs.addAll(recordIds)
            }
            override fun delete(recordId: String) = DelStatus.OK
            override fun mutate(record: LocalRecordAtts) = record.id
        })

        records.register(RecordsDaoProxy(proxyId, targetId))

        val testWithSourceId = { sourceId: String ->
            RequestContext.doWithTxn {
                records.mutate("$sourceId@rec0", emptyMap<Any, Any>())
                records.mutate("$sourceId@rec1", emptyMap<Any, Any>())
                records.mutate("$sourceId@rec2", emptyMap<Any, Any>())
            }
            assertThat(commitRecs).containsExactly("rec0", "rec1", "rec2")
            assertThat(rollbackRecs).isEmpty()
            commitRecs.clear()

            val exception = assertThrows<Exception> {
                RequestContext.doWithTxn {
                    records.mutate("$sourceId@rec0", emptyMap<Any, Any>())
                    records.mutate("$sourceId@rec1", emptyMap<Any, Any>())
                    records.mutate("$sourceId@rec2", emptyMap<Any, Any>())
                    error("expected error")
                }
            }
            assertThat(commitRecs).isEmpty()
            assertThat(rollbackRecs).containsExactly("rec0", "rec1", "rec2")
            assertThat(exception.message).isEqualTo("expected error")
            rollbackRecs.clear()
        }

        testWithSourceId(targetId)
        testWithSourceId(proxyId)
    }

    @Test
    fun deleteTest() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi? {
                return EcosWebAppApiMock("test")
            }
        }
        val records = services.recordsServiceV1

        records.register(TxnDao())

        RequestContext.doWithTxn(readOnly = false) {
            repeat(10) {
                records.create(
                    TxnDao.ID,
                    mapOf(
                        "key" to "value-$it",
                        "_localId" to "rec-$it"
                    )
                )
            }
        }

        val ref = RecordRef.valueOf("${TxnDao.ID}@rec-0")
        RequestContext.doWithTxn(readOnly = false) {
            assertThat(records.getAtt(ref, "key").asText()).isEqualTo("value-0")
            records.delete(ref)
            assertThat(records.getAtt(ref, "key").asText()).isEqualTo("")
        }

        RequestContext.doWithTxn(readOnly = true) {
            assertThat(
                records.query(
                    RecordsQuery.create {
                        withSourceId(TxnDao.ID)
                        withQuery(VoidPredicate.INSTANCE)
                    }
                ).getTotalCount()
            ).isEqualTo(9)
        }

        RequestContext.doWithTxn(readOnly = false) {
            records.delete(listOf("rec-1", "rec-2", "rec-3").map { RecordRef.create(TxnDao.ID, it) })
        }

        RequestContext.doWithTxn(readOnly = true) {
            assertThat(
                records.query(
                    RecordsQuery.create {
                        withSourceId(TxnDao.ID)
                        withQuery(VoidPredicate.INSTANCE)
                    }
                ).getTotalCount()
            ).isEqualTo(6)
        }
    }

    @Test
    fun localTest() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1
        records.register(TxnDao())

        testImpl(records, TxnDao.ID)

        records.register(TxnDao())
        records.register(RecordsDaoProxy("proxy-id", TxnDao.ID))

        testImpl(records, "proxy-id")
    }

    @Test
    fun remoteTest() {

        val remoteAppName = "remoteApp"
        val remoteServices = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock(remoteAppName)
            }
        }
        remoteServices.recordsServiceV1.register(TxnDao())

        val localServices = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                val context = object : EcosWebAppApiMock("test") {
                    override fun getWebClientApi(): EcosWebClientApi {
                        return object : EcosWebClientApi {
                            override fun <R : Any> execute(
                                targetApp: String,
                                path: String,
                                version: Int,
                                request: Any,
                                respType: Class<R>
                            ): Promise<R> {
                                val restHandler = remoteServices.restHandlerAdapter
                                val result = AtomicReference<Any>()
                                thread(start = true) {
                                    result.set(
                                        when (path) {
                                            RemoteRecordsResolver.QUERY_PATH -> restHandler.queryRecords(request)
                                            RemoteRecordsResolver.MUTATE_PATH -> restHandler.mutateRecords(request)
                                            RemoteRecordsResolver.DELETE_PATH -> restHandler.deleteRecords(request)
                                            RemoteRecordsResolver.TXN_PATH -> restHandler.txnAction(request)
                                            else -> error("Unknown path: '$path'")
                                        }
                                    )
                                }.join()
                                return Promises.resolve(Json.mapper.convert(result.get(), respType)!!)
                            }
                        }
                    }
                }
                return context
            }
        }
        RequestContext.setDefaultServices(localServices)
        testImpl(localServices.recordsServiceV1, remoteAppName + "/" + TxnDao.ID)

        remoteServices.recordsServiceV1.register(TxnDao())

        localServices.recordsServiceV1.register(
            RecordsDaoProxy("proxy-id", remoteAppName + "/" + TxnDao.ID)
        )
        testImpl(localServices.recordsServiceV1, "proxy-id")
    }

    private fun testImpl(records: RecordsService, sourceId: String) {

        val testRef = RecordRef.valueOf("$sourceId@test-rec")
        val attName = "attName"

        var attValue = records.getAtt(testRef, attName)
        assertThat(attValue.isNull()).isTrue

        val attValueSrc = "attribute value"
        RequestContext.doWithTxn {

            records.mutate(testRef, mapOf(attName to attValueSrc))

            attValue = records.getAtt(testRef, attName)
            assertThat(attValue.asText()).isEqualTo(attValueSrc)
        }

        attValue = records.getAtt(testRef, attName)
        assertThat(attValue.asText()).isEqualTo(attValueSrc)

        val attValue2Src = "attribute value 2"
        try {
            RequestContext.doWithTxn {
                records.mutate(testRef, mapOf(attName to attValue2Src))
                attValue = records.getAtt(testRef, attName)
                assertThat(attValue.asText()).isEqualTo(attValue2Src)
                error("Some error")
            }
        } catch (e: Exception) {
            // expected error
        }

        attValue = records.getAtt(testRef, attName)
        assertThat(attValue.asText()).isEqualTo(attValueSrc)
    }

    class TxnDao : RecordMutateDao, RecordDeleteDao, RecordAttsDao, TxnRecordsDao, RecordsQueryDao {

        companion object {
            const val ID = "txn-test"
        }

        private val records = mutableMapOf<String, LocalRecordAtts>()

        private val txnRecords = mutableMapOf<UUID, MutableMap<String, LocalRecordAtts>>()
        private val txnDelRecords = mutableMapOf<UUID, MutableSet<String>>()

        override fun getId() = ID

        override fun queryRecords(recsQuery: RecordsQuery): Any {
            return records.values.map { it.attributes }
        }

        override fun getRecordAtts(recordId: String): Any? {
            val txnId = RequestContext.getCurrentNotNull().ctxData.txnId
            if (txnId != null) {
                if ((txnDelRecords[txnId] ?: mutableSetOf()).contains(recordId)) {
                    return null
                }
                return ((this.txnRecords[txnId] ?: mutableMapOf())[recordId] ?: records[recordId])?.attributes
            } else {
                return records[recordId]?.attributes
            }
        }

        override fun delete(recordId: String): DelStatus {
            val txnId = RequestContext.getCurrentNotNull().ctxData.txnId ?: error("txnId is null")
            txnDelRecords.computeIfAbsent(txnId) { HashSet() }.add(recordId)
            return DelStatus.OK
        }

        override fun mutate(record: LocalRecordAtts): String {
            val txnId = RequestContext.getCurrentNotNull().ctxData.txnId ?: error("txnId is null")
            val id = record.attributes["_localId"].asText()
                .ifBlank { record.id }
                .ifBlank { UUID.randomUUID().toString() }
            txnRecords.computeIfAbsent(txnId) { HashMap() }[id] = record
            return id
        }

        override fun commit(txnId: UUID, recordIds: List<String>) {
            txnRecords[txnId]?.forEach { (id, atts) ->
                if (recordIds.contains(id)) {
                    records[id] = atts
                } else {
                    error(
                        "Record $id was changed in transaction " +
                            "but not received in commit method. Records: $recordIds"
                    )
                }
            }
            txnRecords.remove(txnId)
            txnDelRecords[txnId]?.forEach {
                if (recordIds.contains(it)) {
                    records.remove(it)
                } else {
                    error(
                        "Record $it was deleted in transaction " +
                            "but not received in commit method. Records: $recordIds"
                    )
                }
            }
            txnDelRecords.remove(txnId)
        }

        override fun rollback(txnId: UUID, recordIds: List<String>) {
            txnRecords.remove(txnId)
            txnDelRecords.remove(txnId)
        }
    }

    @AfterEach
    fun doAfterEach() {
        RequestContext.setDefaultServices(null)
    }
}
