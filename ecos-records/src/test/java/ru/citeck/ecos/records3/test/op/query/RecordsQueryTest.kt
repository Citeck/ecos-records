package ru.citeck.ecos.records3.test.op.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import kotlin.test.assertEquals

class RecordsQueryTest {

    @Test
    fun removeAfterIdTest() {

        val srcData = DataValue.create(
            """
            {
                "query":{
                    "page": {
                        "afterId": "abc"
                    }
                }
            }
            """.trimIndent()
        )
        assertThat(srcData["query"]["page"].has("afterId")).isTrue
        val newData = srcData.copy().remove("$.query.page.afterId")
        assertThat(newData["query"]["page"].has("afterId")).isFalse
        val newData2 = newData.copy().remove("$.query.page.afterId")
        assertThat(newData2["query"]["page"].has("afterId")).isFalse

        val dataWithAfterId = srcData.copy()
        dataWithAfterId["query"]["page"]["afterId"] = "abc"
        assertThat(dataWithAfterId).isEqualTo(srcData)
    }

    @Test
    fun test() {

        val query = RecordsQuery.create()
            .withPage(
                QueryPage.create {
                    withMaxItems(10)
                    withSkipCount(20)
                }
            ).build()

        val v0Query = Json.mapper.convert(query, RecordsQuery::class.java)!!
        assertEquals(10, v0Query.page.maxItems)
        assertEquals(20, v0Query.page.skipCount)
    }

    @Test
    fun toStringTest() {

        val query = RecsQueryRes.of("One", "Two", "Three", TestObj(), TestObjErr())
        assertEquals(
            "{\"records\":[One,Two,Three,TestObj," +
                "\"toString error: err\\\"abc\\\"\"" +
                "],\"hasMore\":false,\"totalCount\":0}",
            query.toString()
        )
    }

    @Test
    fun strCollectionResultTest() {

        val records = RecordsServiceFactory().recordsService
        fun createDao(srcId: String): RecordsDao = object : RecordsQueryDao, RecordAttsDao {
            override fun getId(): String {
                return srcId
            }
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                return listOf("some-id")
            }
            override fun getRecordAtts(recordId: String): Any? {
                if (recordId == "some-id") {
                    return DataValue.create("""{"aa":"bb"}""")
                }
                return null
            }
        }
        listOf("test", "app/test").forEach { sourceId ->
            records.register(createDao(sourceId))
            val res = records.query(RecordsQuery.create { withSourceId(sourceId) }, listOf("aa"))
            assertThat(res.getRecords()).describedAs("src: $sourceId").hasSize(1)
            assertThat(res.getRecords()[0]["aa"].asText()).describedAs("src: $sourceId").isEqualTo("bb")
        }
    }

    class TestObj {
        override fun toString(): String {
            return "TestObj"
        }
    }
    class TestObjErr {
        override fun toString(): String {
            error("err\"abc\"")
        }
    }
}
