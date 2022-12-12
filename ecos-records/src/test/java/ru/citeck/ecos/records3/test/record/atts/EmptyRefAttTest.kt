package ru.citeck.ecos.records3.test.record.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.test.EcosWebAppApiMock
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.webapp.api.EcosWebAppApi

class EmptyRefAttTest {

    companion object {
        const val APP_NAME = "test-app"
    }

    @Test
    fun test() {

        val services = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return EcosWebAppApiMock(APP_NAME)
            }
        }
        val records = services.recordsServiceV1

        val value = RecordValue()
        records.register(
            RecordsDaoBuilder.create("test")
                .addRecord("rec0", value)
                .build()
        )

        val ref = RecordRef.create("test", "rec0")

        val compareStrAtt = { att: String, expected: String ->
            assertThat(records.getAtt(ref, att).asText()).describedAs(att).isEqualTo(expected)
        }

        compareStrAtt("emptyRef?id", "")
        compareStrAtt("emptyStr?id", "")
        compareStrAtt("refWithoutApp?id", APP_NAME + "/" + value.refWithoutApp)
        compareStrAtt("refWithOtherApp?id", value.refWithOtherApp.toString())
        compareStrAtt("refWithApp?id", value.refWithApp.toString())
    }

    class RecordValue(
        val emptyRef: RecordRef = RecordRef.EMPTY,
        val emptyStr: String = "",
        val refWithoutApp: RecordRef = RecordRef.create("source-id", "local-id"),
        val refWithOtherApp: RecordRef = RecordRef.create("otherApp", "source-id", "local-id"),
        val refWithApp: RecordRef = RecordRef.create(APP_NAME, "source-id", "local-id"),
    )
}
