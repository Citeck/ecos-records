package ru.citeck.ecos.records3.test.record.dao.impl.mem

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class InMemRecordsDaoTest {

    companion object {
        private const val ID = "test-id"
    }

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao(ID))

        val rec0 = records.mutate(
            RecordRef.create(ID, ""),
            ObjectData.create(
                """
            {
                "field0": "value0",
                "field1": "value1"
            }
        """
            )
        )

        assertThat(rec0.id).isNotBlank
        assertThat(records.getAtt(rec0, "field0").asText()).isEqualTo("value0")
        assertThat(records.getAtt(rec0, "field1").asText()).isEqualTo("value1")

        records.mutateAtt(rec0, "field0", "test")
        assertThat(records.getAtt(rec0, "field0").asText()).isEqualTo("test")
        assertThat(records.getAtt(rec0, "field1").asText()).isEqualTo("value1")

        records.delete(rec0)

        assertThat(records.getAtt(rec0, "field0").isNull()).isTrue
        assertThat(records.getAtt(rec0, "field1").isNull()).isTrue

        val recordsToCreate = Array(10) { idx ->
            val data = ObjectData.create()
                .set("idx", idx)
            RecordAtts(RecordRef.create(ID, ""), data)
        }.toList()

        val recs = records.mutate(recordsToCreate)

        val queryRes = records.query(
            RecordsQuery.create {
                withSourceId(ID)
            }
        )
        assertThat(queryRes.getRecords()).hasSize(recordsToCreate.size)

        val queryRes2 = records.query(
            RecordsQuery.create {
                withSourceId(ID)
                withQuery(Predicates.ge("idx", 5))
            }
        )
        assertThat(queryRes2.getRecords()).hasSize(
            recordsToCreate.filter {
                it.getAtt("idx").asInt() >= 5
            }.size
        )

        val queryRes3 = records.query(RecordsQuery.create { withSourceId(ID) }, listOf("idx"))
        assertThat(queryRes3.getRecords().map { it.getAtt("idx").asInt() })
            .containsExactlyInAnyOrderElementsOf(recordsToCreate.indices)
    }
}
