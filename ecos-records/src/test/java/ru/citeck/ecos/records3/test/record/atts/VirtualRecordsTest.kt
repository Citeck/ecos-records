package ru.citeck.ecos.records3.test.record.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef

class VirtualRecordsTest {

    @Test
    fun test() {

        val recordsDao = InMemRecordsDao<TestDto>("test")
        val services = RecordsServiceFactory()

        val records = services.recordsServiceV1
        records.register(recordsDao)

        val resolver = services.localRecordsResolver

        recordsDao.setRecord("realRec", TestDto("abc"))

        resolver.registerVirtualRecord(
            EntityRef.create("test", "virtRec"),
            TestDto("def")
        )

        val atts = records.getAtts(
            listOf(
                EntityRef.valueOf("test@realRec"),
                EntityRef.valueOf("test@virtRec")
            ),
            mapOf(
                "field" to "field"
            )
        )

        assertThat(atts[0]["field"].asText()).isEqualTo("abc")
        assertThat(atts[1]["field"].asText()).isEqualTo("def")
    }

    class TestDto(
        val field: String
    )
}
