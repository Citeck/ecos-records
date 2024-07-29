package ru.citeck.ecos.records3.test.record.dao.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.test.testutils.MockApp
import ru.citeck.ecos.records3.test.testutils.MockAppsFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RemoteMutationTest {

    @Test
    fun associationsWithAliasesTest() {

        val appsFactory = MockAppsFactory()

        val gateway = appsFactory.createGatewayApp()
        val otherApp = appsFactory.createApp("other-app")

        val app0 = appsFactory.createApp("app0")
        val app1 = appsFactory.createApp("app1")

        val app0Records = app0.factory.recordsService
        val app1Records = app1.factory.recordsService

        val mutatedRecords = Array(2) { mutableMapOf<String, LocalRecordAtts>() }
        val toMutatedId = { id: String -> "$id-mutated" }

        listOf(app0Records, app1Records).forEachIndexed { idx, records ->
            records.register(object : RecordMutateDao {
                override fun getId(): String = "dao-id"
                override fun mutate(record: LocalRecordAtts): String {
                    mutatedRecords[idx][record.id] = record
                    return toMutatedId.invoke(record.id)
                }
            })
        }

        val recAlias1 = "alias-01"
        val recAlias2 = "alias-02"
        val record0 = RecordAtts(EntityRef.create("app0", "dao-id", "local-id"))
        record0.setAtt("test-assoc?assoc", recAlias1)
        record0.setAtt("test-assoc-arr?assoc", listOf(recAlias1))
        record0.setAtt(".att(n:\"legacy-assoc\"){assoc}", listOf(recAlias1, recAlias1))
        record0.setAtt("test-assoc-rec2?assoc", recAlias2)

        val record1 = RecordAtts(EntityRef.create("app1", "dao-id", "local-id"))
        record1.setAtt(RecordConstants.ATT_ALIAS, recAlias1)

        val record2 = RecordAtts(EntityRef.create("app1", "dao-id", "local-id-2"))
        record2.setAtt(RecordConstants.ATT_ALIAS + "?str", recAlias2)

        val sourceRecs = listOf(record0, record1, record2).map { it.deepCopy() }

        val mutateWithApp = { app: MockApp ->
            mutatedRecords.forEach { it.clear() }

            val records = app.factory.recordsService
            val result = records.mutate(listOf(record0, record1, record2))
            val expectedRec1Id = toMutatedId.invoke("app1/dao-id@local-id")
            val expectedRec2Id = toMutatedId.invoke("app1/dao-id@local-id-2")
            assertThat(result[1].toString()).isEqualTo(expectedRec1Id)

            assertThat(result).hasSize(3)
            assertThat(result[0].getLocalId()).isEqualTo(toMutatedId.invoke("local-id"))
            assertThat(result[1].getLocalId()).isEqualTo(toMutatedId.invoke("local-id"))
            assertThat(result[2].getLocalId()).isEqualTo(toMutatedId.invoke("local-id-2"))

            assertThat(mutatedRecords[0]).hasSize(1)
            assertThat(mutatedRecords[0]["local-id"]).isNotNull
            assertThat(mutatedRecords[1]).hasSize(2)
            assertThat(mutatedRecords[1]["local-id"]).isNotNull
            assertThat(mutatedRecords[1]["local-id-2"]).isNotNull

            val record0AfterMutationAtts = mutatedRecords[0]["local-id"]!!.attributes
            assertThat(record0AfterMutationAtts.get("test-assoc", "")).isEqualTo(expectedRec1Id)
            assertThat(record0AfterMutationAtts.get("test-assoc-arr").asStrList()).isEqualTo(listOf(expectedRec1Id))
            assertThat(record0AfterMutationAtts.get("legacy-assoc").asStrList()).isEqualTo(listOf(expectedRec1Id, expectedRec1Id))
            assertThat(record0AfterMutationAtts.get("test-assoc-rec2", "")).isEqualTo(expectedRec2Id)
        }

        mutateWithApp(gateway)
        mutateWithApp(otherApp)

        assertThat(sourceRecs).containsExactly(record0, record1, record2)
    }
}
