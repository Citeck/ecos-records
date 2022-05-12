package ru.citeck.ecos.records3.test.record.atts.schema.resolver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.test.testutils.WebAppContextMock
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext

class SchemaResolverIdTest {

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppContext(): EcosWebAppContext {
                return WebAppContextMock("test-app")
            }
        }
        val records = services.recordsServiceV1

        val recordsDao = RecordsDaoBuilder.create("test")
            .addRecord("some-id", ObjectData.create())
            .build() as InMemRecordsDao<*>

        records.register(recordsDao)

        val id = records.getAtt(RecordRef.create("test", "some-id"), "?id").asText()
        assertThat(id).isEqualTo(services.webappProps.appName + "/test@some-id")
    }
}
