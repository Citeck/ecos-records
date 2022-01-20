package ru.citeck.ecos.records3.test.record.resolver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.test.testutils.MockAppsFactory

class RemoteRecordsApiTest {

    @Test
    fun test() {

        val appsFactory = MockAppsFactory()

        val app0 = appsFactory.createApp("app-0")
        val app1 = appsFactory.createApp("app-1")

        app1.factory.recordsServiceV1.register(
            RecordsDaoBuilder.create("test")
                .addRecord("test", Record("str"))
                .build()
        )

        val requestRemoteAtt = {
            val strFieldValue = app0.factory.recordsServiceV1
                .getAtt("app-1/test@test", "strField?str")
                .asText()
            assertThat(strFieldValue).isEqualTo("str")
        }

        requestRemoteAtt()

        val checkRequests = { v2Expected: Boolean, query: Boolean ->

            assertThat(appsFactory.requests).hasSize(2)
            val req0 = appsFactory.requests[0]
            assertThat(req0.bodyObj.get("$.records[0]").asText()).isEqualTo("api@")
            assertThat(req0.bodyObj.get("version").asText()).isEqualTo("1")
            assertThat(req0.bodyObj.get("v").asText()).isEmpty()

            val req1 = appsFactory.requests[1]
            if (query) {
                assertThat(req1.bodyObj.get("query").isObject()).isTrue
                assertThat(req1.bodyObj.get("$.records").asStrList()).isEmpty()
            } else {
                assertThat(req1.bodyObj.get("query").isNull()).isTrue
                assertThat(req1.bodyObj.get("$.records[0]").asText()).isEqualTo("test@test")
            }
            if (v2Expected) {
                assertThat(req1.bodyObj.get("version").asText()).isEmpty()
                assertThat(req1.bodyObj.get("v").asText()).isEqualTo("2")
            } else {
                assertThat(req1.bodyObj.get("version").asText()).isEqualTo("1")
                assertThat(req1.bodyObj.get("v").asText()).isEmpty()
            }
        }

        checkRequests(true, false)

        appsFactory.requests.clear()
        requestRemoteAtt()
        assertThat(appsFactory.requests).hasSize(1)

        appsFactory.requests.clear()

        val apiRecordsDao = app1.factory.recordsServiceV1.getRecordsDao("api")!!
        app1.factory.recordsServiceV1.unregister("api")
        app0.factory.cacheManager.clearAll()

        requestRemoteAtt()
        checkRequests(false, false)

        appsFactory.requests.clear()
        app0.factory.cacheManager.clearAll()
        app1.factory.recordsServiceV1.register(apiRecordsDao)

        val queryRemoteApp = { afterId: Boolean ->
            val queryRes = app0.factory.recordsServiceV1.query(
                RecordsQuery.create {
                    withSourceId("${app1.name}/test")
                    withQuery(Predicates.eq("strField", "str"))
                    if (afterId) {
                        withAfterId(RecordRef.EMPTY)
                    }
                }
            )
            assertThat(queryRes.getRecords()).hasSize(1)
            assertThat(queryRes.getTotalCount()).isEqualTo(1)
            assertThat(queryRes.getRecords()[0]).isEqualTo(RecordRef.valueOf("app-1/test@test"))
        }

        queryRemoteApp(true)
        checkRequests(true, true)
        assertThat(appsFactory.requests[1].bodyObj.get("$.query.page.afterId").isNull()).isFalse

        appsFactory.requests.clear()
        app0.factory.cacheManager.clearAll()
        app1.factory.recordsServiceV1.unregister("api")

        queryRemoteApp(false)
        checkRequests(false, true)
        assertThat(appsFactory.requests[1].bodyObj.get("$.query.page.afterId").isNull()).isTrue

        println(app0.factory.recordsServiceV1.getAtt("api@", "cache.stats?json"))
    }

    class Record(
        val strField: String
    )
}
