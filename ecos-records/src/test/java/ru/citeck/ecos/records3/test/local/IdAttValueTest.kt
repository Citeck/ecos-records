package ru.citeck.ecos.records3.test.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.test.testutils.WebAppContextMock
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext

class IdAttValueTest {

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppContext(): EcosWebAppContext {
                return WebAppContextMock("app0")
            }
        }
        val records = services.recordsServiceV1

        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("first", TestDto("first"))
                .build()
        )

        val res = records.getAtt("test@first", "?id").asText()
        assertThat(res).isEqualTo(services.webappProps.appName + "/test@first")
    }

    class TestDto(val id: String)
}
