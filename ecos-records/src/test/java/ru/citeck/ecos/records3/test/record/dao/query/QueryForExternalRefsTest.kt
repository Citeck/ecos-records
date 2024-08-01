package ru.citeck.ecos.records3.test.record.dao.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef

class QueryForExternalRefsTest {

    @Test
    fun test() {

        val records = RecordsServiceFactory().recordsService

        records.register(
            RecordsDaoBuilder.create("main")
                .addRecord("first", mapOf("aa" to "bb"))
                .build()
        )

        records.register(object : RecordsQueryDao {
            override fun queryRecords(recsQuery: RecordsQuery): Any {
                return listOf(EntityRef.valueOf("main@first"))
            }
            override fun getId() = "ext"
        })

        val res = records.query(
            RecordsQuery.create()
                .withSourceId("ext")
                .build()
        ).getRecords()

        assertThat(res).containsExactly(EntityRef.valueOf("main@first"))
    }
}
