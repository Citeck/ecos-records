package ru.citeck.ecos.records3.test.record.atts.value

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class EmptyAssocTest {

    @Test
    fun test() {

        val idScalars = listOf("assoc", "id", "localId")

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("test", RecordValue())
                .build()
        )

        records.register(object : RecordsQueryDao {
            override fun getId() = "records-with-empty-id"
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                return ObjectData.create()
            }
        })

        val testIdGeneratorForRecWithoutId = { att: String ->
            val atts = records.query(
                RecordsQuery.create {
                    withSourceId("records-with-empty-id")
                },
                mapOf(att to att)
            ).getRecords()[0]
            val value = atts.getAtt(att).asText()
            assertThat(value).describedAs(att).isNotBlank()
            assertThat(value).describedAs(att).isNotEqualTo("records-with-empty-id@")
        }
        idScalars.forEach {
            testIdGeneratorForRecWithoutId("?$it")
        }

        assertThat(records.getAtt("test@test", "?id").asText()).isNotBlank()

        listOf(
            "emptyStr",
            "objData.missingField",
            "objData.emptyStr"
        ).forEach { att ->
            idScalars.forEach { scalar ->
                val singleValueAtt = "$att?$scalar"
                assertThat(records.getAtt("test@test", singleValueAtt).asText())
                    .describedAs(singleValueAtt)
                    .isBlank()
                val multiValueAtt = "$att[]?$scalar"
                assertThat(records.getAtt("test@test", multiValueAtt).asStrList())
                    .describedAs(multiValueAtt)
                    .isEmpty()
            }
        }
    }

    class RecordValue(
        val objData: ObjectData = ObjectData.create(mapOf("emptyStr" to "")),
        val emptyStr: String = ""
    )
}
