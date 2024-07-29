package ru.citeck.ecos.records2.test.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

class MetaValueAttValueTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsService
        val legacyRecords = services.recordsService

        legacyRecords.register(object : RecordAttsDao {

            override fun getRecordAtts(recordId: String): Any {
                return EntityRef.create("test", recordId)
            }
            override fun getId(): String {
                return "legacy"
            }
        })

        records.register(InMemDataRecordsDao("test"))
        val ref = records.create("test", mapOf("aaa" to "bbb"))

        assertThat(records.getAtt(ref, "aaa").asText()).isEqualTo("bbb")
        assertThat(records.getAtt(EntityRef.create("legacy", ref.getLocalId()), "aaa").asText()).isEqualTo("bbb")
    }
}
