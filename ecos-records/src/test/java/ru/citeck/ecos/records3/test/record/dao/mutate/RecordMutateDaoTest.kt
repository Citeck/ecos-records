package ru.citeck.ecos.records3.test.record.dao.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordMutateDaoTest {

    private val mutatedList = mutableListOf<RecordAtts>()

    @Test
    fun appNameTest() {
        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock("test")
            }
        }
        val records = services.recordsService
        records.register(InMemDataRecordsDao("test"))

        val res = records.create("test", mapOf("id" to "123"))
        assertThat(res).isEqualTo(EntityRef.create("test", "test", "123"))
    }

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi? {
                return EcosWebAppApiMock("test-app")
            }
        }
        val records = services.recordsService

        // simple mutate

        records.register(object : RecordMutateDao {
            override fun getId() = "test-mutate"
            override fun mutate(record: LocalRecordAtts): String {
                mutatedList.add(
                    RecordAtts(
                        EntityRef.create("test-app", getId(), record.id),
                        record.attributes
                    )
                )
                return record.id
            }
        })

        mutateTest("test-mutate", records)

        // mutate with pseudo remote dao

        records.register(object : RecordMutateDao {
            override fun getId() = "remote/test-mutate2"
            override fun mutate(record: LocalRecordAtts): String {
                mutatedList.add(
                    RecordAtts(
                        EntityRef.create("remote", getId().substringAfterLast('/'), record.id),
                        record.attributes
                    )
                )
                return record.id
            }
        })

        mutateTest("remote/test-mutate2", records)

        // mutate with legacy dao

        services.recordsService.register(object : RecordMutateDao {

            override fun mutate(record: LocalRecordAtts): String {
                mutatedList.add(
                    RecordAtts(
                        EntityRef.create("test-app", getId(), record.id),
                        record.getAtts()
                    )
                )
                return record.id
            }

            override fun getId() = "legacy-dao"
        })

        mutateTest("legacy-dao", records)
    }

    private fun mutateTest(sourceId: String, records: RecordsService) {

        mutatedList.clear()

        val attsToMutate = ObjectData.create(
            """
                    {
                        "test": "atts",
                        "bool": true,
                        "num": 1234
                    }
            """.trimIndent()
        )

        if (!sourceId.contains('/')) {
            val firstRecordAtts = RecordAtts(EntityRef.valueOf("test-app/$sourceId@localId"), attsToMutate)
            records.mutate(firstRecordAtts)
            assertThat(mutatedList).containsExactly(firstRecordAtts)
        }

        mutatedList.clear()

        val secondRecordAtts = RecordAtts(EntityRef.valueOf("$sourceId@localId2"), attsToMutate)

        records.mutate(secondRecordAtts)
        val expectedRec = if (!sourceId.contains("/")) {
            secondRecordAtts.withId(secondRecordAtts.getId().withDefaultAppName("test-app"))
        } else {
            secondRecordAtts
        }
        assertThat(mutatedList).containsExactly(expectedRec)
    }
}
