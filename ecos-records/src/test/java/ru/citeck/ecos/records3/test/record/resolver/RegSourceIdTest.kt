package ru.citeck.ecos.records3.test.record.resolver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.test.EcosWebAppApiMock
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.webapp.api.EcosWebAppApi

class RegSourceIdTest {

    @Test
    fun test() {
        val appName = "test-app"
        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock(appName)
            }
        }
        val records = services.recordsServiceV1
        records.register(InMemDataRecordsDao("$appName/test"))

        val ref = records.create("test", mapOf("aa" to "bb"))

        assertThat(records.getAtt(ref.withSourceId("test"), "aa").asText()).isEqualTo("bb")
        assertThat(records.getAtt(ref.withSourceId("$appName/test"), "aa").asText()).isEqualTo("bb")
    }
}
