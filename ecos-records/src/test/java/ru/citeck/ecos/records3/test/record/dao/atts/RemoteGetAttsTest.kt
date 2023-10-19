package ru.citeck.ecos.records3.test.record.dao.atts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.test.testutils.MockApp
import ru.citeck.ecos.records3.test.testutils.MockAppsFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RemoteGetAttsTest {

    @Test
    fun test() {

        val appsFactory = MockAppsFactory()
        val apps = Array(2) {
            appsFactory.createApp("app-$it")
        }
        val gateway = appsFactory.createGatewayApp(defaultApp = "app-0")

        val fieldValue = "field"
        apps[0].factory.recordsServiceV1.register(
            RecordsDaoBuilder.create("test")
                .addRecord("rec0", RecordData(fieldValue))
                .build()
        )

        val assertAtt = { withApp: Boolean, app: MockApp, expected: String ->
            val ref = if (withApp) {
                EntityRef.create("app-0", "test", "rec0")
            } else {
                EntityRef.create("test", "rec0")
            }
            val value = app.factory.recordsServiceV1.getAtt(ref, "field").asText()
            assertThat(value).isEqualTo(expected)
        }

        assertAtt(false, apps[0], fieldValue)
        assertAtt(true, apps[0], fieldValue)

        assertAtt(false, apps[1], "")
        assertAtt(true, apps[1], fieldValue)

        assertAtt(false, gateway, fieldValue)
        assertAtt(true, gateway, fieldValue)
    }

    class RecordData(
        val field: String
    )
}
