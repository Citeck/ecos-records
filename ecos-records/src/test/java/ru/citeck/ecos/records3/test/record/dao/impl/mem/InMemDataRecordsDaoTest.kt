package ru.citeck.ecos.records3.test.record.dao.impl.mem

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

class InMemDataRecordsDaoTest {

    companion object {
        private const val ID = "test-id"
    }

    @Test
    fun testWithPredicates() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao("test"))

        val ref0 = records.create("test", mapOf("abc" to "def"))
        val ref1 = records.create("test", mapOf("abc" to "def"))

        val baseQuery = RecordsQuery.create()
            .withSourceId("test")
            .withMaxItems(10)
            .withQuery(Predicates.alwaysTrue())
            .build()

        val res0 = records.query(baseQuery).getRecords()

        assertThat(res0).containsExactly(ref0, ref1)

        assertThat(records.getAtt(ref0, "?id").asText()).isEqualTo(ref0.toString())
        assertThat(records.getAtt(ref0, "id").asText()).isEqualTo(ref0.getLocalId())
        assertThat(records.getAtt(ref0, "id?str").asText()).isEqualTo(ref0.getLocalId())
        assertThat(records.getAtt(ref0, "id?raw").asText()).isEqualTo(ref0.getLocalId())

        val res1 = records.query(
            baseQuery.copy()
                .withQuery(Predicates.notEq("id", ref0.getLocalId()))
                .build()
        ).getRecords()

        assertThat(res1).containsExactly(ref1)
    }

    @Test
    fun testWithCopyAndUpdateById() {

        val records = RecordsServiceFactory().recordsServiceV1

        records.register(InMemDataRecordsDao("test"))
        val res0 = records.create(
            "test",
            mapOf(
                "id" to "abc",
                "name" to "ABC"
            )
        )
        assertThat(res0.getLocalId()).isEqualTo("abc")
        assertThat(records.getAtt(res0, "name").asText()).isEqualTo("ABC")

        val res1 = records.mutate(
            EntityRef.valueOf("test@"),
            mapOf(
                "id" to "abc",
                "name" to "ABCDEF"
            )
        )
        assertThat(res1).isEqualTo(res0)
        assertThat(records.getAtt(res0, "name").asText()).isEqualTo("ABCDEF")
    }

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao(ID))

        val rec0 = records.mutate(
            EntityRef.create(ID, ""),
            ObjectData.create(
                """
            {
                "field0": "value0",
                "field1": "value1"
            }
        """
            )
        )

        assertThat(rec0.getLocalId()).isNotBlank
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
            RecordAtts(EntityRef.create(ID, ""), data)
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
    fun simpleSearchTest() {

        val records = RecordsServiceFactory().recordsServiceV1
        records.register(InMemDataRecordsDao("test"))

        val refs = (0 until 300).map {
            val createdDate = Instant.ofEpochMilli(10000L * it)
            val recordRef = records.create(
                "test",
                mapOf(
                    "_created" to createdDate
                )
            )
            recordRef to createdDate
        }

        val conditionValue = Instant.ofEpochMilli(10000L * 100)

        val result = records.query(
            RecordsQuery.create()
                .withSourceId("test")
                .withQuery(Predicates.gt("_created", conditionValue))
                .withSortBy(SortBy("_created", true))
                .withMaxItems(1000)
                .build()
        )

        assertThat(result.getRecords()).hasSize(199)
        assertThat(result.getRecords()).containsExactlyElementsOf(
            refs.filter {
                it.second.isAfter(conditionValue)
            }.map {
                it.first
            }
        )
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
            return records.query(query).getRecords().map { it.getLocalId().toInt() }
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
