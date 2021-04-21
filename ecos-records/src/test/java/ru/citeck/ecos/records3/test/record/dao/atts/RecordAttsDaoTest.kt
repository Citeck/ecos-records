package ru.citeck.ecos.records3.test.record.dao.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao

class RecordAttsDaoTest {

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun createProperties(): RecordsProperties {
                val props = super.createProperties()
                props.appName = "test-app"
                return props
            }
        }
        val records = services.recordsServiceV1

        // simple atts

        records.register(object : RecordAttsDao {
            override fun getId() = "test-atts"
            override fun getRecordAtts(recordId: String): Any {
                return ObjectData.create("""{"test":"value"}""")
            }
        })

        attsTest("test-atts", records)

        // atts with pseudo remote dao

        records.register(object : RecordAttsDao {
            override fun getId() = "remote/test-atts2"
            override fun getRecordAtts(recordId: String): Any {
                return ObjectData.create("""{"test":"value"}""")
            }
        })

        attsTest("remote/test-atts2", records)

        // atts with legacy dao

        services.recordsService.register(object : LocalRecordsDao(), LocalRecordsMetaDao<Any> {
            override fun getLocalRecordsMeta(records: MutableList<RecordRef>, metaField: MetaField): List<Any> {
                return records.map { ObjectData.create("""{"test":"value"}""") }
            }
            override fun getId() = "legacy-dao"
        })

        attsTest("legacy-dao", records)
    }

    private fun attsTest(sourceId: String, records: RecordsService) {

        if (!sourceId.contains('/')) {
            val res = records.getAtt(RecordRef.valueOf("test-app/$sourceId@localId"), "test").asText()
            assertThat(res).isEqualTo("value")
        }

        val res = records.getAtt(RecordRef.valueOf("$sourceId@localId2"), "test").asText()
        assertThat(res).isEqualTo("value")
    }
}
