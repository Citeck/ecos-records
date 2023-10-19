package ru.citeck.ecos.records3.test.op.mutate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.records3.test.testutils.MockAppsFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef

class CreateWithAssocsTest {

    @Test
    fun mutationTestWithLink() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao("test"))

        val recordsToMutate = listOf(
            RecordAtts(
                EntityRef.valueOf("test@"),
                ObjectData.create("""{"link":"alias-1","links":["alias-1","alias-1"]}""")
            ),
            RecordAtts(
                EntityRef.valueOf("test@"),
                ObjectData.create("""{"_alias":"alias-1"}""")
            )
        )

        val mutRes = records.mutate(recordsToMutate)
        assertThat(mutRes).hasSize(2)
        assertThat(records.getAtt(mutRes[0], "link?id").asText()).isEqualTo(mutRes[1].toString())
        assertThat(records.getAtt(mutRes[0], "links[]?id").asStrList()).containsExactly(
            mutRes[1].toString(),
            mutRes[1].toString()
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["?assoc", "?str", ""])
    fun test(assocScalar: String) {

        val rec1Alias = "source-id@-alias-1"

        val rec0 = RecordAtts(EntityRef.create("source-id", ""))
        rec0.setAtt("assocArr$assocScalar", listOf(rec1Alias))
        rec0.setAtt("assocStr$assocScalar", rec1Alias)

        val rec1 = RecordAtts(EntityRef.create("source-id", ""))
        rec1.setAtt("_alias", "source-id@-alias-1")

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao("source-id"))

        val mutRes = records.mutate(listOf(rec0, rec1))

        val assocStrRes = records.getAtt(mutRes[0], "assocStr?id").asText()
        assertThat(assocStrRes).isEqualTo(mutRes[1].toString())
        val assocArrRes = records.getAtt(mutRes[0], "assocArr[]?id").asStrList()[0]
        assertThat(assocArrRes).isEqualTo(mutRes[1].toString())
    }

    @Test
    fun testWithDefaultAppName() {

        val appsFactory = MockAppsFactory()

        val targetAppName = "test-app"
        val testSourceId = "test-source-id"

        val gateway = appsFactory.createGatewayApp(defaultApp = targetAppName)
        val targetApp = appsFactory.createApp(targetAppName)
        targetApp.factory.recordsServiceV1.register(InMemDataRecordsDao(testSourceId))

        val alias = "test-alias-1"
        val fieldWithAssoc = "assocField"

        val rec0 = RecordAtts(EntityRef.create(targetAppName, testSourceId, ""))
        rec0.setAtt(fieldWithAssoc, listOf(alias))

        // rec with default app
        val rec1 = RecordAtts(EntityRef.create(testSourceId, ""))
        rec1.setAtt(RecordConstants.ATT_ALIAS, alias)

        val result = gateway.factory.recordsServiceV1.mutate(listOf(rec0, rec1))

        assertThat(result.size).isEqualTo(2)
        val assocFieldValue = gateway.factory
            .recordsServiceV1.getAtt(result[0], "$fieldWithAssoc?id")
            .getAs(EntityRef::class.java)

        assertThat(assocFieldValue).isEqualTo(result[1])
    }
}
