package ru.citeck.ecos.records3.test.record.resolver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.test.EcosWebAppContextMock
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext

class RegSourceIdTest {

    @Test
    fun test() {
        val appName = "test-app"
        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppContext(): EcosWebAppContext {
                return EcosWebAppContextMock(appName)
            }
        }
        val records = services.recordsServiceV1
        records.register(InMemDataRecordsDao("$appName/test"))

        val ref = records.create("test", mapOf("aa" to "bb"))

        assertThat(records.getAtt(ref.withSourceId("test"), "aa").asText()).isEqualTo("bb")
        assertThat(records.getAtt(ref.withSourceId("$appName/test"), "aa").asText()).isEqualTo("bb")
    }
}
