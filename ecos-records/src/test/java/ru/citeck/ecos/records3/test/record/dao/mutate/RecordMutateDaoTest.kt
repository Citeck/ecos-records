package ru.citeck.ecos.records3.test.record.dao.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.test.EcosWebAppContextMock
import ru.citeck.ecos.records2.RecordMeta
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.request.delete.RecordsDelResult
import ru.citeck.ecos.records2.request.delete.RecordsDeletion
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult
import ru.citeck.ecos.records2.request.mutation.RecordsMutation
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext

class RecordMutateDaoTest {

    private val mutatedList = mutableListOf<RecordAtts>()

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppContext(): EcosWebAppContext? {
                return EcosWebAppContextMock("test-app")
            }
        }
        val records = services.recordsServiceV1

        // simple mutate

        records.register(object : RecordMutateDao {
            override fun getId() = "test-mutate"
            override fun mutate(record: LocalRecordAtts): String {
                mutatedList.add(
                    RecordAtts(
                        RecordRef.create("test-app", getId(), record.id),
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
                        RecordRef.create("remote", getId().substringAfterLast('/'), record.id),
                        record.attributes
                    )
                )
                return record.id
            }
        })

        mutateTest("remote/test-mutate2", records)

        // mutate with legacy dao

        services.recordsService.register(object : LocalRecordsDao(), MutableRecordsLocalDao<Any> {
            override fun delete(deletion: RecordsDeletion): RecordsDelResult {
                error("Not implemented")
            }

            override fun mutateImpl(mutation: RecordsMutation): RecordsMutResult {
                mutation.records.forEach {
                    mutatedList.add(
                        RecordAtts(
                            RecordRef.create("test-app", id, it.getId().id),
                            it.getAttributes()
                        )
                    )
                }
                val result = RecordsMutResult()
                result.records = mutation.records.map { RecordMeta(it) }
                return result
            }

            override fun getValuesToMutate(records: MutableList<RecordRef>): MutableList<Any> {
                error("Not implemented")
            }

            override fun save(values: MutableList<Any>): RecordsMutResult {
                error("Not implemented")
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
            val firstRecordAtts = RecordAtts(RecordRef.valueOf("test-app/$sourceId@localId"), attsToMutate)
            records.mutate(firstRecordAtts)
            assertThat(mutatedList).containsExactly(firstRecordAtts)
        }

        mutatedList.clear()

        val secondRecordAtts = RecordAtts(RecordRef.valueOf("$sourceId@localId2"), attsToMutate)

        records.mutate(secondRecordAtts)
        val expectedRec = if (!sourceId.contains("/")) {
            secondRecordAtts.withId(secondRecordAtts.getId().withDefaultAppName("test-app"))
        } else {
            secondRecordAtts
        }
        assertThat(mutatedList).containsExactly(expectedRec)
    }
}
