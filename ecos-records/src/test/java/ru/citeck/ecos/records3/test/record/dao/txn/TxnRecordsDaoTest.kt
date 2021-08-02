package ru.citeck.ecos.records3.test.record.dao.txn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
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
    fun localTest() {

        val services = RecordsServiceFactory()
        RequestContext.setDefaultServices(services)
        val records = services.recordsServiceV1
        records.register(TxnDao())

        testImpl(records, TxnDao.ID)
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
            override fun createRemoteRecordsResolver(): RemoteRecordsResolver? {
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
    }

    private fun testImpl(records: RecordsService, sourceId: String) {

        val testRef = RecordRef.valueOf("$sourceId@test-rec")
        val attName = "attName"

        var attValue = records.getAtt(testRef, attName)
        assertThat(attValue.isNull()).isTrue()

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

    class TxnDao : RecordMutateDao, RecordDeleteDao, RecordAttsDao, TxnRecordsDao {

        companion object {
            const val ID = "txn-test"
        }

        private val records = mutableMapOf<String, LocalRecordAtts>()

        private val txnRecords = mutableMapOf<UUID, MutableMap<String, LocalRecordAtts>>()
        private val txnDelRecords = mutableMapOf<UUID, MutableSet<String>>()

        override fun getId() = ID

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
            txnRecords.computeIfAbsent(txnId) { HashMap() }[record.id] = record
            return record.id
        }

        override fun commit(txnId: UUID, recordsId: List<String>) {
            txnRecords[txnId]?.forEach { (id, atts) -> records[id] = atts }
            txnRecords.remove(txnId)
            txnDelRecords[txnId]?.forEach { records.remove(it) }
            txnDelRecords.remove(txnId)
        }

        override fun rollback(txnId: UUID, recordsId: List<String>) {
            txnRecords.remove(txnId)
            txnDelRecords.remove(txnId)
        }
    }
}
