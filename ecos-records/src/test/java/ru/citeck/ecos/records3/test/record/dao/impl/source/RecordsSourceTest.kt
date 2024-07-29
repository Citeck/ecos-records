package ru.citeck.ecos.records3.test.record.dao.impl.source

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.impl.source.client.ClientMeta
import ru.citeck.ecos.records3.record.dao.impl.source.client.HasClientMeta

class RecordsSourceTest {

    @Test
    fun test() {

        val recordsDaoOnlyWithAtts = object : RecordAttsDao {
            override fun getId() = "recordsDaoOnlyWithAtts"
            override fun getRecordAtts(recordId: String) = error("")
        }

        val records = RecordsServiceFactory().recordsService
        records.register(recordsDaoOnlyWithAtts)

        assertThat(
            records.getAtt(
                "source@recordsDaoOnlyWithAtts",
                "features.getAtts?bool"
            ).asBoolean()
        ).isTrue()

        assertThat(
            records.getAtt(
                "src@recordsDaoOnlyWithAtts",
                "features.getAtts?bool"
            ).asBoolean()
        ).isTrue()

        assertThat(
            records.getAtt(
                "source@recordsDaoOnlyWithAtts",
                "features.mutate?bool"
            ).asBoolean()
        ).isFalse()
    }

    @Test
    fun clientMetaTest() {

        val expectedClientMeta = ClientMeta(
            "clientMeta",
            ObjectData.create(
                """
            {
            "field0": "value0",
            "field1": 123
            }
                """.trimIndent()
            )
        )

        val recordsDaoWithClientMeta = object : RecordAttsDao, HasClientMeta {
            override fun getId() = "recordsDaoWithClientMeta"
            override fun getRecordAtts(recordId: String) = error("")
            override fun getClientMeta(): ClientMeta = expectedClientMeta
        }

        val recordsDaoWithoutClientMeta = object : RecordAttsDao {
            override fun getId() = "recordsDaoWithoutClientMeta"
            override fun getRecordAtts(recordId: String) = error("")
        }

        val records = RecordsServiceFactory().recordsService
        records.register(recordsDaoWithClientMeta)
        records.register(recordsDaoWithoutClientMeta)

        val clientConfig = records.getAtt("src@recordsDaoWithClientMeta", "client.config?json").asObjectData()
        assertThat(expectedClientMeta.config).isEqualTo(clientConfig)

        val clientType = records.getAtt("src@recordsDaoWithClientMeta", "client.type").asText()
        assertThat(expectedClientMeta.type).isEqualTo(clientType)

        val emptyClientConfig = records.getAtt("src@recordsDaoWithoutClientMeta", "client.config?json")
        assertThat(emptyClientConfig.isNull()).isTrue()

        val emptyClientType = records.getAtt("src@recordsDaoWithoutClientMeta", "client.type")
        assertThat(emptyClientType.isNull()).isTrue()
    }
}
