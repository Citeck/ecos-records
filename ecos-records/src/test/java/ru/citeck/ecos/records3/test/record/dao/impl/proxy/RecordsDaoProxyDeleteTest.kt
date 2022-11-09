package ru.citeck.ecos.records3.test.record.dao.impl.proxy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.records3.record.dao.impl.proxy.DeleteProxyProcessor
import ru.citeck.ecos.records3.record.dao.impl.proxy.ProxyProcContext
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy

class RecordsDaoProxyDeleteTest {

    companion object {
        const val PROXY_ID = "proxy-id"
        const val TARGET_ID = "target-id"
    }

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        val recordsMap = HashMap<String, ObjectData>()
        recordsMap["rec0"] = ObjectData.create("""{"aa":"bb"}""")
        recordsMap["rec1"] = ObjectData.create("""{"cc":"cc"}""")
        recordsMap["rec2"] = ObjectData.create("""{"dd":"dd"}""")

        records.register(object : RecordsDeleteDao {
            override fun getId() = TARGET_ID
            override fun delete(recordIds: List<String>): List<DelStatus> {
                recordIds.forEach { recordsMap.remove(it) }
                return recordIds.map { DelStatus.OK }
            }
        })

        val preDeleteList = mutableListOf<List<String>>()
        val postDeleteList = mutableListOf<List<String>>()

        val proc = object : DeleteProxyProcessor {
            override fun deletePreProcess(recordIds: List<String>, context: ProxyProcContext) {
                preDeleteList.add(recordIds)
            }
            override fun deletePostProcess(
                recordIds: List<String>,
                statuses: List<DelStatus>,
                context: ProxyProcContext
            ) {
                postDeleteList.add(recordIds)
            }
        }

        records.register(RecordsDaoProxy(PROXY_ID, TARGET_ID, proc))

        val proxyRec0Ref = RecordRef.create(PROXY_ID, "rec0")
        val proxyRec1Ref = RecordRef.create(PROXY_ID, "rec1")
        val proxyRec2Ref = RecordRef.create(PROXY_ID, "rec2")

        assertThat(recordsMap).hasSize(3)

        records.delete(proxyRec0Ref)

        assertThat(recordsMap).hasSize(2)
        assertThat(preDeleteList).containsExactly(listOf(proxyRec0Ref.id))
        assertThat(postDeleteList).containsExactly(listOf(proxyRec0Ref.id))

        preDeleteList.clear()
        postDeleteList.clear()

        records.delete(listOf(proxyRec1Ref, proxyRec2Ref))

        assertThat(recordsMap).isEmpty()

        assertThat(preDeleteList.flatten()).containsExactlyElementsOf(listOf(proxyRec1Ref, proxyRec2Ref).map { it.id })
        assertThat(postDeleteList.flatten()).containsExactlyElementsOf(listOf(proxyRec1Ref, proxyRec2Ref).map { it.id })
    }
}
