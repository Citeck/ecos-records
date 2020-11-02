package ru.citeck.ecos.records3.test.op.query

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.op.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import kotlin.test.assertEquals

class RecordsQueryTest {

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
}
