package ru.citeck.ecos.records3.test.record.dao.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.test.testutils.MockAppsFactory

class GatewayMutationTest {

    @Test
    fun test() {

        val appsFactory = MockAppsFactory()

        val gateway = appsFactory.createApp("gateway", gatewayMode = true)

        val app0 = appsFactory.createApp("app0")
        val app1 = appsFactory.createApp("app1")

        val app0Records = app0.factory.recordsServiceV1
        val app1Records = app1.factory.recordsServiceV1

        val mutatedRecords = Array(2) { mutableMapOf<String, LocalRecordAtts>() }

        listOf(app0Records, app1Records).forEachIndexed { idx, records ->
            records.register(object : RecordMutateDao {
                override fun getId(): String = "dao-id"
                override fun mutate(record: LocalRecordAtts): String {
                    mutatedRecords[idx][record.id] = record
                    return record.id + "-mutated"
                }
            })
        }

        val recAlias1 = "alias-01"
        val recAlias2 = "alias-02"
        val record0 = RecordAtts(RecordRef.create("app0", "dao-id", "local-id"))
        record0.setAtt("test-assoc?assoc", recAlias1)
        record0.setAtt("test-assoc-arr?assoc", listOf(recAlias1))
        record0.setAtt(".att(n:\"legacy-assoc\"){assoc}", listOf(recAlias1, recAlias1))
        record0.setAtt("test-assoc-rec2?assoc", recAlias2)

        val record1 = RecordAtts(RecordRef.create("app1", "dao-id", "local-id"))
        record1.setAtt(RecordConstants.ATT_ALIAS, recAlias1)

        val record2 = RecordAtts(RecordRef.create("app1", "dao-id", "local-id-2"))
        record2.setAtt(RecordConstants.ATT_ALIAS + "?str", recAlias2)

        val gatewayRecords = gateway.factory.recordsServiceV1
        val result = gatewayRecords.mutate(listOf(record0, record1, record2))
        val expectedRec1Id = "app1/dao-id@local-id-mutated"
        val expectedRec2Id = "app1/dao-id@local-id-2-mutated"
        assertThat(result[1].toString()).isEqualTo(expectedRec1Id)

        assertThat(result).hasSize(3)
        assertThat(result[0].id).isEqualTo("local-id-mutated")
        assertThat(result[1].id).isEqualTo("local-id-mutated")
        assertThat(result[2].id).isEqualTo("local-id-2-mutated")

        assertThat(mutatedRecords[0]).hasSize(1)
        assertThat(mutatedRecords[0]["local-id"]).isNotNull
        assertThat(mutatedRecords[1]).hasSize(2)
        assertThat(mutatedRecords[1]["local-id"]).isNotNull
        assertThat(mutatedRecords[1]["local-id-2"]).isNotNull

        val record0AfterMutation = mutatedRecords[0]["local-id"]!!
        assertThat(record0AfterMutation.attributes.get("test-assoc", "")).isEqualTo(expectedRec1Id)
        assertThat(record0AfterMutation.attributes.get("test-assoc-arr").asStrList()).isEqualTo(listOf(expectedRec1Id))
        assertThat(record0AfterMutation.attributes.get("legacy-assoc").asStrList()).isEqualTo(listOf(expectedRec1Id, expectedRec1Id))
        assertThat(record0AfterMutation.attributes.get("test-assoc-rec2", "")).isEqualTo(expectedRec2Id)
    }
}
