package ru.citeck.ecos.records2.test.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao

class MetaValueAttValueTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1
        val legacyRecords = services.recordsService

        legacyRecords.register(object : LocalRecordsDao(), LocalRecordsMetaDao<Any> {
            override fun getLocalRecordsMeta(records: MutableList<RecordRef>, metaField: MetaField): List<Any> {
                return records.map { RecordRef.create("test", it.id) }
            }
            override fun getId(): String {
                return "legacy"
            }
        })

        records.register(InMemDataRecordsDao("test"))
        val ref = records.create("test", mapOf("aaa" to "bbb"))

        assertThat(records.getAtt(ref, "aaa").asText()).isEqualTo("bbb")
        assertThat(records.getAtt(RecordRef.create("legacy", ref.id), "aaa").asText()).isEqualTo("bbb")
    }
}
