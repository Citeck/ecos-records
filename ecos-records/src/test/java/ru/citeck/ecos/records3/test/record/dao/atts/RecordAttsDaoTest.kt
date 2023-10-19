package ru.citeck.ecos.records3.test.record.dao.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

class RecordAttsDaoTest {

    @Test
    fun unorderedAttsTest() {

        data class Record(
            val id: String,
            val value: String
        )

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(object : RecordsAttsDao, RecordsQueryDao {
            override fun getId(): String {
                return "test"
            }
            override fun queryRecords(recsQuery: RecordsQuery): Any? {
                return recsQuery.getQuery(DataValue::class.java)["refs"].asList(EntityRef::class.java)
            }
            override fun getRecordsAtts(recordIds: List<String>): List<*> {
                return recordIds.shuffled(Random(12346789L)).map { Record(it, "$it-value") }
            }
        })

        val recordIds = (0 until 10).map { "test@record-$it" }

        val getAttsRes = records.getAtts(recordIds, listOf("value"))
        val getAttsResult = getAttsRes.map { it["value"].asText() }

        assertThat(getAttsResult).containsExactlyElementsOf(recordIds.map { "${it.substringAfter('@')}-value" })

        val queryRes = records.query(
            RecordsQuery.create {
                withSourceId("test")
                withQuery(DataValue.createObj().set("refs", recordIds))
            },
            listOf("value")
        ).getRecords().map { it["value"].asText() }

        assertThat(queryRes).containsExactlyElementsOf(recordIds.map { "${it.substringAfter('@')}-value" })
    }

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock("test-app")
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
            override fun getLocalRecordsMeta(records: MutableList<EntityRef>, metaField: MetaField): List<Any> {
                return records.map { ObjectData.create("""{"test":"value"}""") }
            }
            override fun getId() = "legacy-dao"
        })

        attsTest("legacy-dao", records)
    }

    private fun attsTest(sourceId: String, records: RecordsService) {

        if (!sourceId.contains('/')) {
            val res = records.getAtt(EntityRef.valueOf("test-app/$sourceId@localId"), "test").asText()
            assertThat(res).isEqualTo("value")
        }

        val res = records.getAtt(EntityRef.valueOf("$sourceId@localId2"), "test").asText()
        assertThat(res).isEqualTo("value")
    }

    @Test
    fun notExistsTest() {

        val recordAttsDao = object : RecordAttsDao {
            override fun getId() = "test"
            override fun getRecordAtts(recordId: String): Any? {
                if (recordId == "null") {
                    return null
                }
                return recordId
            }
        }
        val records = RecordsServiceFactory().recordsServiceV1
        records.register(recordAttsDao)
        assertThat(records.getAtt("test@null", "_notExists?bool").asBoolean()).isTrue
        assertThat(records.getAtt("test@other", "_notExists?bool").asBoolean()).isFalse

        val recordsAttsDao = object : RecordsAttsDao {
            override fun getId() = "test2"
            override fun getRecordsAtts(recordIds: List<String>): List<Any?> {
                return recordIds.map {
                    if (it == "null") {
                        null
                    } else {
                        it
                    }
                }
            }
        }
        records.register(recordsAttsDao)
        assertThat(records.getAtt("test2@null", "_notExists?bool").asBoolean()).isTrue
        assertThat(records.getAtt("test2@other", "_notExists?bool").asBoolean()).isFalse
    }
}
