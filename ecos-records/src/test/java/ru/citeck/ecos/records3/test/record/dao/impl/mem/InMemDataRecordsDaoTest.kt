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
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy

class InMemDataRecordsDaoTest {

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

    @Test
    fun sortByTest() {
        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1
        val recordsDao = InMemDataRecordsDao("test")
        records.register(recordsDao)

        repeat(10) {
            records.create(
                "test",
                mapOf(
                    "id" to it,
                    "num" to it,
                    "num_mod_3" to it % 3
                )
            )
        }
        val query = RecordsQuery.create {
            withSourceId("test")
        }
        fun queryImpl(query: RecordsQuery): List<Int> {
            return records.query(query).getRecords().map { it.id.toInt() }
        }

        val queryRes0 = queryImpl(query.copy { withQuery(Predicates.eq("num", 5)) })
        assertThat(queryRes0).hasSize(1)
        assertThat(queryRes0[0]).isEqualTo(5)

        val queryRes1 = queryImpl(
            query.copy {
                withQuery(Predicates.gt("num", 5))
                withSortBy(listOf(SortBy("num", true)))
            }
        )
        assertThat(queryRes1).containsExactly(6, 7, 8, 9)

        val queryRes2 = queryImpl(
            query.copy {
                withQuery(Predicates.gt("num", 5))
                withSortBy(listOf(SortBy("num", false)))
            }
        )
        assertThat(queryRes2).containsExactly(9, 8, 7, 6)

        val queryRes3 = queryImpl(
            query.copy {
                withQuery(Predicates.gt("num", 5))
                withSortBy(listOf(SortBy("num", false)))
                withSkipCount(1)
                withMaxItems(2)
            }
        )
        assertThat(queryRes3).containsExactly(8, 7)

        val queryRes4 = queryImpl(
            query.copy {
                withQuery(Predicates.ge("num", 1))
                withSortBy(
                    listOf(
                        SortBy("num_mod_3", true),
                        SortBy("num", true)
                    )
                )
            }
        )
        assertThat(queryRes4).containsExactly(
            3, // 0
            6, // 0
            9, // 0
            1, // 1
            4, // 1
            7, // 1
            2, // 2
            5, // 2
            8, // 2
        )

        val queryRes5 = queryImpl(
            query.copy {
                withQuery(Predicates.ge("num", 1))
                withSortBy(
                    listOf(
                        SortBy("num_mod_3", false),
                        SortBy("num", true)
                    )
                )
            }
        )
        assertThat(queryRes5).containsExactly(
            2, // 2
            5, // 2
            8, // 2
            1, // 1
            4, // 1
            7, // 1
            3, // 0
            6, // 0
            9 // 0
        )
    }
}
