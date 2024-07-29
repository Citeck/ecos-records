package ru.citeck.ecos.records3.test.record.atts

import io.github.oshai.kotlinlogging.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDao
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.impl.NullAttValue
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import ru.citeck.ecos.records3.test.testutils.MockAppsFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

class ContextAttsTest {

    companion object {
        private const val LOCAL_APP_NAME = "local"
        private const val REMOTE_APP_NAME = "remote"

        private val log = KotlinLogging.logger {}
    }

    @Test
    fun testForEmptyValue() {

        val factory = RecordsServiceFactory()
        val records = factory.recordsService

        val attName = "attName"
        val ctxAttName = "ctxAttName"
        val ctxValue = "ctxValue"

        val testWithCtxValue = { record: Any?, withCtxAtts: Boolean ->

            // get atts for record
            val value = if (withCtxAtts) {
                RequestContext.doWithAtts(mapOf(ctxAttName to ctxValue)) { _ ->
                    records.getAtts(record, listOf(attName, "\$$ctxAttName"))
                }
            } else {
                records.getAtts(record, listOf(attName, "\$$ctxAttName"))
            }
            assertThat(value.getAtts().size()).isEqualTo(2)
            assertThat(value.hasAtt(attName)).isTrue
            assertThat(value.hasAtt("\$$ctxAttName")).isTrue
            assertThat(value.getAtt(attName).isNull()).isTrue
            if (withCtxAtts) {
                assertThat(value.getAtt("\$$ctxAttName", "")).isEqualTo(ctxValue)
            } else {
                assertThat(value.getAtt("\$$ctxAttName").isNull()).isTrue
            }

            // getAtt for record
            val valueWithGetAtt = if (withCtxAtts) {
                RequestContext.doWithAtts(mapOf(ctxAttName to ctxValue)) { _ ->
                    records.getAtt(record, "\$$ctxAttName")
                }
            } else {
                records.getAtt(record, "\$$ctxAttName")
            }
            if (withCtxAtts) {
                assertThat(valueWithGetAtt.asText()).isEqualTo(ctxValue)
            } else {
                assertThat(valueWithGetAtt.isNull()).isTrue
            }

            // getAttsForRecords
            val valueWithGetAttsForRecords = if (withCtxAtts) {
                RequestContext.doWithAtts(mapOf(ctxAttName to ctxValue)) { _ ->
                    records.getAtts(listOf(record), listOf("\$$ctxAttName"))
                }
            } else {
                records.getAtts(listOf(record), listOf("\$$ctxAttName"))
            }
            assertThat(valueWithGetAttsForRecords).hasSize(1)
            assertThat(valueWithGetAttsForRecords[0].getAtts().has("\$$ctxAttName")).isTrue
            val ctxAttVal = valueWithGetAttsForRecords[0].getAtts().get("\$$ctxAttName")
            if (withCtxAtts) {
                assertThat(ctxAttVal.asText()).isEqualTo(ctxValue)
            } else {
                assertThat(ctxAttVal.isNull()).isTrue
            }
        }

        listOf(EntityRef.EMPTY, NullAttValue.INSTANCE, null).forEach { record ->
            try {
                testWithCtxValue.invoke(record, false)
                testWithCtxValue.invoke(record, true)
            } catch (e: Throwable) {
                val clazz = if (record != null) {
                    record::class.simpleName
                } else {
                    "null"
                }
                log.error { "Record: '$record' with class $clazz" }
                throw e
            }
        }
    }

    @Test
    fun testWithRemoteApp() {

        val mockAppsFactory = MockAppsFactory()

        val localApp = mockAppsFactory.createApp(LOCAL_APP_NAME)
        val remoteApp = mockAppsFactory.createApp(REMOTE_APP_NAME)

        val localRecord = EntityRef.create(LOCAL_APP_NAME, MetaRecordsDao.ID, "")
        val remoteRecord = EntityRef.create(REMOTE_APP_NAME, MetaRecordsDao.ID, "")

        val ctxAttValue = "ctx-att-value"
        RequestContext.doWithAtts(mapOf("ctxAtt" to ctxAttValue)) { _ ->
            val records = localApp.factory.recordsService
            assertThat(records.getAtt(localRecord, "\$ctxAtt").asText()).isEqualTo(ctxAttValue)
            assertThat(records.getAtt(remoteRecord, "\$ctxAtt").asText()).isEqualTo(ctxAttValue)
        }

        // ===================

        val gatewayApp = mockAppsFactory.createGatewayApp()
        val queryBody = QueryBody()
        queryBody.rawAtts = false
        queryBody.attributes = ObjectData.create()
            .set("attKey", "\$ctxAtt")
            .getData()
        queryBody.setRecords(listOf(remoteRecord))

        val queryResp0 = gatewayApp.getRestHandlerV1().queryRecords(queryBody)
        assertThat(queryResp0.records[0].getAtt("attKey", "")).isEmpty()

        remoteApp.ctxAtts["ctxAtt"] = ctxAttValue
        val queryResp1 = gatewayApp.getRestHandlerV1().queryRecords(queryBody)
        assertThat(queryResp1.records[0].getAtt("attKey", "")).isEqualTo(ctxAttValue)
    }

    @Test
    fun globalCtxAttsCallsCountTest() {

        val recsCount = 10

        var getAttCallsCount = 0
        val recordsToGetAtts = Array(recsCount) {
            object : AttValue {
                override fun getAtt(name: String): Any? {
                    getAttCallsCount++
                    if (name == "field") {
                        return "value-$it"
                    }
                    return null
                }
            }
        }.toList()

        val records = RecordsServiceFactory().recordsService
        val atts = records.getAtts(recordsToGetAtts, listOf("field"))

        assertThat(getAttCallsCount).isEqualTo(recsCount)
        for (i in atts.indices) {
            assertThat(atts[i].getAtt("field", "")).isEqualTo("value-$i")
        }

        getAttCallsCount = 0
        var getRecordAttsCallsCount = 0
        records.register(object : RecordAttsDao {
            override fun getId() = "test"
            override fun getRecordAtts(recordId: String): Any {
                getRecordAttsCallsCount++
                return recordsToGetAtts[recordId.toInt()]
            }
        })

        val recordRefsToGetAtts = Array(recsCount) { EntityRef.create("test", it.toString()) }.toList()

        val atts2 = records.getAtts(recordRefsToGetAtts, listOf("field"))

        assertThat(getAttCallsCount).isEqualTo(recsCount)
        assertThat(getRecordAttsCallsCount).isEqualTo(recsCount)
        for (i in atts2.indices) {
            assertThat(atts2[i].getAtt("field", "")).isEqualTo("value-$i")
        }

        getAttCallsCount = 0
        getRecordAttsCallsCount = 0

        val contextValueFieldValue = "value-ctx"
        var contextValueCallsCount = 0
        val contextValue = object : AttValue {
            override fun getAtt(name: String): Any? {
                contextValueCallsCount++
                if (name == "field") {
                    return contextValueFieldValue
                }
                return null
            }
        }

        val ctxAttAlias = "ctx-alias"
        val attsWithCtxField = RequestContext.doWithAtts(
            mapOf(
                "ctxAtt" to contextValue
            )
        ) { _ ->
            records.getAtts(recordRefsToGetAtts, mapOf(ctxAttAlias to "\$ctxAtt.field"))
        }

        assertThat(attsWithCtxField).hasSize(recordRefsToGetAtts.size)
        attsWithCtxField.forEach {
            assertThat(it.getAtt(ctxAttAlias, "")).isEqualTo(contextValueFieldValue)
        }
        assertThat(contextValueCallsCount).isEqualTo(1)
        assertThat(getAttCallsCount).isEqualTo(0)
        assertThat(getRecordAttsCallsCount).isEqualTo(0)
    }

    @Test
    fun postProcTest() {

        val records = RecordsServiceFactory().recordsService

        val currentYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR)
        val year1 = records.getAtt(null, "\$now|fmt('yyyy')").asInt()
        assertThat(year1).isEqualTo(currentYear)
        val year2 = records.getAtt(null, "\$now?raw|fmt('yyyy')").asInt()
        assertThat(year2).isEqualTo(currentYear)
    }
}
