package ru.citeck.ecos.records3.test.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi

class IdAttValueTest {

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock("app0")
            }
        }
        val records = services.recordsService

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
