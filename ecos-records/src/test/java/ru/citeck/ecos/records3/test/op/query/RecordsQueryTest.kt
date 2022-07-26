package ru.citeck.ecos.records3.test.op.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import kotlin.test.assertEquals

class RecordsQueryTest {

    @Test
    fun removeAfterIdTest() {

        val queryBody = QueryBody()
        queryBody.query = RecordsQuery.create {}

        val srcData = DataValue.create(queryBody)
        assertThat(srcData.get("query").get("page").has("afterId")).isTrue
        val newData = srcData.copy().remove("$.query.page.afterId")
        assertThat(newData.get("query").get("page").has("afterId")).isFalse
        val newData2 = newData.copy().remove("$.query.page.afterId")
        assertThat(newData2.get("query").get("page").has("afterId")).isFalse

        val dataWithAfterId = srcData.copy()
        dataWithAfterId.get("query").get("page").set("afterId", "")
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

        val v0Query = Json.mapper.convert(query, ru.citeck.ecos.records2.request.query.RecordsQuery::class.java)!!
        assertEquals(10, v0Query.maxItems)
        assertEquals(20, v0Query.skipCount)
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
