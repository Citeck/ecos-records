package ru.citeck.ecos.records3.test.record.dao.impl.proxy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.records3.record.dao.impl.proxy.MutateProxyProcessor
import ru.citeck.ecos.records3.record.dao.impl.proxy.ProxyProcContext
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.request.RequestContext

class RecordsDaoProxyProcTest {

    @Test
    fun testWithActionBeforeCommit() {

        val proxySourceId = "proxy"
        val targetSourceId = "target"

        val mainRecRef = RecordRef.create(targetSourceId, "main")
        val childRecRef = RecordRef.create(targetSourceId, "child")
        val proxyMainRecRef = mainRecRef.withSourceId(proxySourceId)
        val proxyChildRecRef = childRecRef.withSourceId(proxySourceId)

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        val targetDao = InMemDataRecordsDao(targetSourceId)
        records.register(targetDao)
        val targetRecords = targetDao.getRecords()

        val proxyDao = RecordsDaoProxy(
            proxySourceId, targetSourceId,
            object : MutateProxyProcessor {
                override fun mutatePreProcess(
                    atts: List<LocalRecordAtts>,
                    context: ProxyProcContext
                ): List<LocalRecordAtts> {
                    if (atts[0].attributes.get("id", "") == "child") {
                        assertThat(targetRecords).isEmpty()
                        RequestContext.getCurrentNotNull().doBeforeCommit {
                            assertThat(RequestContext.getCurrentNotNull().ctxData.txnId).isNotNull
                            assertThat(targetRecords).hasSize(2)
                            assertThat(targetRecords["main"]!!.get("field", "")).isEqualTo("main-value")

                            val parentAttValue = records.getAtt(mainRecRef, "field").asText()
                            records.mutateAtt(childRecRef, "parent_att_value", parentAttValue)
                        }
                        RequestContext.getCurrentNotNull().doAfterCommit {
                            assertThat(targetRecords).hasSize(2)
                            assertThat(targetRecords["main"]!!.get("field", "")).isEqualTo("main-value")
                        }
                    }
                    return atts
                }
                override fun mutatePostProcess(records: List<RecordRef>, context: ProxyProcContext): List<RecordRef> {
                    return records
                }
            }
        )
        records.register(proxyDao)

        val mainRecordAtts = RecordAtts(
            RecordRef.create(proxySourceId, ""),
            ObjectData.create(
                """
            {
                "id": "main",
                "assoc?assoc": "alias-01",
                "field": "main-value"
            }
                """.trimIndent()
            )
        )

        val childRecordAtts = RecordAtts(
            RecordRef.create(proxySourceId, ""),
            ObjectData.create(
                """
            {
                "id": "child",
                "_alias": "alias-01",
                "field": "child-value"
            }
                """.trimIndent()
            )
        )

        RequestContext.doWithTxn {
            records.mutate(listOf(mainRecordAtts, childRecordAtts))
        }

        val parentAttValue = records.getAtt(proxyChildRecRef, "parent_att_value").asText()
        assertThat(parentAttValue).isEqualTo("main-value")
    }
}
