package ru.citeck.ecos.records3.test.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsServiceFactory

class IdAttValueTest {

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun createProperties(): RecordsProperties {
                val props = super.createProperties()
                props.appName = "app0"
                props.appInstanceId = props.appName + ":123"
                return props
            }
        }
        val records = services.recordsServiceV1

        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("first", TestDto("first"))
                .build()
        )

        val res = records.getAtt("test@first", "?id").asText()
        assertThat(res).isEqualTo(services.properties.appName + "/test@first")
    }

    class TestDto(val id: String)
}
