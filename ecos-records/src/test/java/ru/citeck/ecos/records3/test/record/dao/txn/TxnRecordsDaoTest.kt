package ru.citeck.ecos.records3.test.record.dao.txn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi
import ru.citeck.ecos.records3.RecordsProperties
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
            override fun commit(txnId: UUID, recordsId: List<String>) {
                commitRecs.addAll(recordsId)
            }
            override fun rollback(txnId: UUID, recordsId: List<String>) {
                rollbackRecs.addAll(recordsId)
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
            override fun createRemoteRecordsResolver(): RemoteRecordsResolver {
                return RemoteRecordsResolver(
                    this,
                    object : RemoteRecordsRestApi {
                        override fun <T : Any?> jsonPost(url: String?, request: Any?, respType: Class<T>?): T {
                            error("Not supported")
                        }
                    }
                )
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
            override fun createProperties(): RecordsProperties {
                val props = super.createProperties()
                props.appName = remoteAppName
                return props
            }
        }
        remoteServices.recordsServiceV1.register(TxnDao())

        val localServices = object : RecordsServiceFactory() {
            override fun createRemoteRecordsResolver(): RemoteRecordsResolver {
                return RemoteRecordsResolver(
                    this,
                    object : RemoteRecordsRestApi {
                        override fun <T : Any> jsonPost(url: String, request: Any, respType: Class<T>): T {
                            val restHandler = remoteServices.restHandlerAdapter
                            val urlPrefix = "/$remoteAppName"
                            val result = AtomicReference<Any>()
                            thread(start = true) {
                                result.set(
                                    when (url) {
                                        urlPrefix + RemoteRecordsResolver.QUERY_URL -> restHandler.queryRecords(request)
                                        urlPrefix + RemoteRecordsResolver.MUTATE_URL -> restHandler.mutateRecords(request)
                                        urlPrefix + RemoteRecordsResolver.DELETE_URL -> restHandler.deleteRecords(request)
                                        urlPrefix + RemoteRecordsResolver.TXN_URL -> restHandler.txnAction(request)
                                        else -> error("Unknown url: '$url'")
                                    }
                                )
                            }.join()
                            return Json.mapper.convert(result.get(), respType)!!
                        }
                    }
                )
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
            val id = record.attributes.get("_localId").asText()
                .ifBlank { record.id }
                .ifBlank { UUID.randomUUID().toString() }
            txnRecords.computeIfAbsent(txnId) { HashMap() }[id] = record
            return id
        }

        override fun commit(txnId: UUID, recordsId: List<String>) {
            txnRecords[txnId]?.forEach { (id, atts) ->
                if (recordsId.contains(id)) {
                    records[id] = atts
                } else {
                    error(
                        "Record $id was changed in transaction " +
                            "but not received in commit method. Records: $recordsId"
                    )
                }
            }
            txnRecords.remove(txnId)
            txnDelRecords[txnId]?.forEach {
                if (recordsId.contains(it)) {
                    records.remove(it)
                } else {
                    error(
                        "Record $it was deleted in transaction " +
                            "but not received in commit method. Records: $recordsId"
                    )
                }
            }
            txnDelRecords.remove(txnId)
        }

        override fun rollback(txnId: UUID, recordsId: List<String>) {
            txnRecords.remove(txnId)
            txnDelRecords.remove(txnId)
        }
    }

    @AfterEach
    fun doAfterEach() {
        RequestContext.setDefaultServices(null)
    }
}