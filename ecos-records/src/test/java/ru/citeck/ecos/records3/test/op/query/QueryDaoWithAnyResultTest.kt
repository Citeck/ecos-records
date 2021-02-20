package ru.citeck.ecos.records3.test.op.query

import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import kotlin.test.assertEquals

class QueryDaoWithAnyResultTest {

    @Test
    fun test() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1
        records.register(RecordsQueryDaoWithAny())

        val nullRes = records.query(
            RecordsQuery.create()
                .withSourceId("test")
                .withLanguage("null")
                .build()
        )
        assertEquals(0, nullRes.getRecords().size)
        assertEquals(0, nullRes.getTotalCount())

        val listRes = records.query(
            RecordsQuery.create()
                .withSourceId("test")
                .withLanguage("list")
                .build()
        )
        assertEquals(3, listRes.getRecords().size)
        assertEquals(RecordRef.valueOf("test@one"), listRes.getRecords()[0])
        assertEquals(RecordRef.valueOf("test@two"), listRes.getRecords()[1])
        assertEquals(RecordRef.valueOf("test@three"), listRes.getRecords()[2])
        assertEquals(3, listRes.getTotalCount())

        val setRes = records.query(
            RecordsQuery.create()
                .withSourceId("test")
                .withLanguage("set")
                .build()
        )
        assertEquals(3, setRes.getRecords().size)
        assertEquals(RecordRef.valueOf("test@four"), setRes.getRecords()[0])
        assertEquals(RecordRef.valueOf("test@five"), setRes.getRecords()[1])
        assertEquals(RecordRef.valueOf("test@six"), setRes.getRecords()[2])
        assertEquals(3, setRes.getTotalCount())

        val mapRes = records.query(
            RecordsQuery.create()
                .withSourceId("test")
                .withLanguage("map")
                .build(),
            listOf("seven", "nine")
        )

        assertEquals(1, mapRes.getRecords().size)
        assertEquals(1, mapRes.getTotalCount())
        assertEquals("eight", mapRes.getRecords()[0].getAtt("seven").asText())
        assertEquals("ten", mapRes.getRecords()[0].getAtt("nine").asText())

        val dtoRes = records.query(
            RecordsQuery.create()
                .withSourceId("test")
                .withLanguage("dto")
                .build(),
            listOf("field0", "field1?num")
        )

        assertEquals(1, dtoRes.getRecords().size)
        assertEquals(1, dtoRes.getTotalCount())
        assertEquals("abc", dtoRes.getRecords()[0].getAtt("field0").asText())
        assertEquals(5.0, dtoRes.getRecords()[0].getAtt("field1?num").asDouble())

        val dtoListRes = records.query(
            RecordsQuery.create()
                .withSourceId("test")
                .withLanguage("dto-list")
                .build(),
            listOf("field0", "field1?num")
        )

        assertEquals(2, dtoListRes.getRecords().size)
        assertEquals(2, dtoListRes.getTotalCount())
        dtoListRes.getRecords().forEach {
            assertEquals("abc", it.getAtt("field0").asText())
            assertEquals(5.0, it.getAtt("field1?num").asDouble())
        }

        val dtoRecsRes = records.query(
            RecordsQuery.create()
                .withSourceId("test")
                .withLanguage("recs-res")
                .build(),
            listOf("field0", "field1?num")
        )

        assertEquals(3, dtoRecsRes.getRecords().size)
        assertEquals(3, dtoRecsRes.getTotalCount())
        dtoRecsRes.getRecords().forEach {
            assertEquals("abc", it.getAtt("field0").asText())
            assertEquals(5.0, it.getAtt("field1?num").asDouble())
        }
    }

    class RecordsQueryDaoWithAny : RecordsQueryDao {

        override fun queryRecords(query: RecordsQuery): Any? {

            return when (query.language) {
                "null" -> null
                "list" -> listOf("one", "two", "three")
                "set" -> setOf("four", "five", "six")
                "dto" -> TestDto()
                "dto-list" -> listOf(TestDto(), TestDto())
                "map" -> mapOf(
                    "seven" to "eight",
                    "nine" to "ten"
                )
                "recs-res" -> RecsQueryRes(listOf(TestDto(), TestDto(), TestDto()))
                else -> error("Unknown language")
            }
        }

        data class TestDto(
            val field0: String = "abc",
            val field1: Int = 5
        )

        override fun getId() = "test"
    }
}
