package ru.citeck.records3.spring.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.impl.proxy.MutateProxyProcessor
import ru.citeck.ecos.records3.record.dao.impl.proxy.ProxyProcContext
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.txn.TxnRecordsDao
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.spring.web.rest.RecordsRestApi
import java.util.*
import kotlin.collections.HashMap

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [TestApp::class])
@Import(TxnTest.Config::class)
class TxnTest {

    @Autowired
    lateinit var records: RecordsService
    @Autowired
    lateinit var recordsDao: Config.RecordsTxnDao
    @Autowired
    lateinit var restApi: RecordsRestApi

    @Test
    fun test() {

        val body = MutateBody()
        val atts = RecordAtts()
        atts.setId(recordsDao.getId() + "@")
        atts.setAtt("test", "value")
        body.addRecord(atts)

        restApi.recordsMutate(Json.mapper.toBytes(body)!!)

        assertThat(recordsDao.txnRecordsMap).isEmpty()
        assertThat(recordsDao.recordsMap).hasSize(1)
        assertThat(recordsDao.recordsMap.values.first().get("test").asText()).isEqualTo("value")

        val proxyDaoId = "txn-dao-proxy"
        var throwError = false
        records.register(
            RecordsDaoProxy(
                proxyDaoId, recordsDao.getId(),
                object : MutateProxyProcessor {
                    override fun mutatePreProcess(
                        atts: List<LocalRecordAtts>,
                        context: ProxyProcContext
                    ): List<LocalRecordAtts> {
                        return atts
                    }
                    override fun mutatePostProcess(records: List<RecordRef>, context: ProxyProcContext): List<RecordRef> {
                        if (throwError) {
                            error("Post process error")
                        } else {
                            return records
                        }
                    }
                }
            )
        )

        recordsDao.recordsMap.clear()
        atts.setId("$proxyDaoId@")
        atts.setAtt("test", "value22")

        restApi.recordsMutate(Json.mapper.toBytes(body)!!)

        assertThat(recordsDao.txnRecordsMap).isEmpty()
        assertThat(recordsDao.recordsMap).hasSize(1)
        assertThat(recordsDao.recordsMap.values.first().get("test").asText()).isEqualTo("value22")

        throwError = true

        recordsDao.recordsMap.clear()
        atts.setId("$proxyDaoId@")
        atts.setAtt("test", "value22")

        restApi.recordsMutate(Json.mapper.toBytes(body)!!)

        assertThat(recordsDao.txnRecordsMap).isEmpty()
        assertThat(recordsDao.recordsMap).isEmpty()
    }

    @Configuration
    open class Config {

        @Component
        class RecordsTxnDao : RecordAttsDao, RecordMutateDao, TxnRecordsDao {

            override fun getId() = "txn-dao"

            val recordsMap = HashMap<String, ObjectData>()
            val txnRecordsMap = HashMap<String, ObjectData>()

            override fun getRecordAtts(recordId: String): Any? {
                return txnRecordsMap[recordId] ?: recordsMap[recordId]
            }

            override fun mutate(record: LocalRecordAtts): String {

                val id = record.id.ifBlank { UUID.randomUUID().toString() }
                var recData = txnRecordsMap[id] ?: recordsMap[id]?.deepCopy()
                if (recData == null) {
                    recData = ObjectData.create()
                }
                record.attributes.forEach { key, value ->
                    recData.set(key, value)
                }
                txnRecordsMap[id] = recData

                return id
            }

            override fun commit(txnId: UUID, recordsId: List<String>) {
                recordsId.forEach {
                    if (txnRecordsMap.containsKey(it)) {
                        recordsMap[it] = txnRecordsMap[it]!!
                        txnRecordsMap.remove(it)
                    }
                }
            }

            override fun rollback(txnId: UUID, recordsId: List<String>) {
                recordsId.forEach {
                    txnRecordsMap.remove(it)
                }
            }
        }
    }
}
