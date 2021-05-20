package ru.citeck.ecos.records3.test.record.dao.impl.proxy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.records3.record.dao.impl.proxy.MutateProxyProcessor
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateDao

class RecordsDaoProxyMutateTest {

    companion object {
        const val PROXY_ID = "proxy-id"
        const val TARGET_ID = "target-id"
    }

    @Test
    fun mutateTest() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        val recordsMap = HashMap<String, ObjectData>()

        records.register(object : RecordsMutateDao, RecordsDeleteDao {
            override fun getId() = TARGET_ID
            override fun mutate(records: List<LocalRecordAtts>): List<String> {
                records.forEach { recordsMap[it.id] = it.attributes }
                return records.map { it.id }
            }
            override fun delete(recordsId: List<String>): List<DelStatus> {
                recordsId.forEach { recordsMap.remove(it) }
                return recordsId.map { DelStatus.OK }
            }
        })
        records.register(RecordsDaoProxy(PROXY_ID, TARGET_ID))

        val proxyRef = RecordRef.create(PROXY_ID, "test")
        val mutRes = records.mutate(
            proxyRef,
            mapOf(
                "field0" to "value0",
                "fieldInt" to 123
            )
        )

        assertThat(mutRes).isEqualTo(proxyRef)

        assertThat(recordsMap).hasSize(1)
        assertThat(recordsMap["test"]).isNotNull
        assertThat(recordsMap["test"]!!.get("field0").asText()).isEqualTo("value0")
        assertThat(recordsMap["test"]!!.get("fieldInt").asInt()).isEqualTo(123)

        val procAtt = "proc-att"
        val procAttValue = "proc-att-value"
        records.register(
            RecordsDaoProxy(
                PROXY_ID, TARGET_ID,
                object : MutateProxyProcessor {
                    override fun prepareMutation(atts: List<LocalRecordAtts>): List<LocalRecordAtts> {
                        return atts.map {
                            val copyAtts = it.attributes.deepCopy()
                            copyAtts.set("field0", null)
                            copyAtts.set(procAtt, procAttValue)
                            LocalRecordAtts(it.id, copyAtts)
                        }
                    }
                }
            )
        )

        val proxyRef2 = RecordRef.create(PROXY_ID, "test2")
        val mutRes2 = records.mutate(
            proxyRef2,
            mapOf(
                "field0" to "value0",
                "fieldInt" to 123
            )
        )

        assertThat(mutRes2).isEqualTo(proxyRef2)

        assertThat(recordsMap).hasSize(2)
        assertThat(recordsMap["test2"]).isNotNull
        assertThat(recordsMap["test2"]!!.get("field0").asText()).isEqualTo("")
        assertThat(recordsMap["test2"]!!.get("fieldInt").asInt()).isEqualTo(123)

        assertThat(recordsMap["test2"]!!.get(procAtt).asText()).isEqualTo(procAttValue)

        records.delete(proxyRef)
        assertThat(recordsMap).hasSize(1)
        assertThat(recordsMap.keys.first()).isEqualTo(proxyRef2.id)
    }
}
