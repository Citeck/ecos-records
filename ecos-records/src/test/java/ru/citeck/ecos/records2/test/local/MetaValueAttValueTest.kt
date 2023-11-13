package ru.citeck.ecos.records2.test.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

class MetaValueAttValueTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1
        val legacyRecords = services.recordsService

        legacyRecords.register(object : LocalRecordsDao(), LocalRecordsMetaDao<Any> {
            override fun getLocalRecordsMeta(records: MutableList<EntityRef>, metaField: MetaField): List<Any> {
                return records.map { EntityRef.create("test", it.getLocalId()) }
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
