package ru.citeck.ecos.records3.test.record.dao.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.lang.Exception

class QueryErrorTest {

    @Test
    fun testAttError() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        records.register(
            RecordsDaoBuilder.create("error-test")
                .addRecord("error-rec", ValueWithErrorAtt())
                .build()
        )

        // local

        val ref = EntityRef.valueOf("error-test@error-rec")

        val ex = assertThrows<Exception> { records.getAtt(ref, "errorAtt") }
        assertThat(ex.message).isEqualTo("ERROR_ATT")

        RequestContext.doWithCtx<Unit>({ it.withOmitErrors(true) }) {
            val valueWithoutError = records.getAtt(ref, "errorAtt")
            assertThat(valueWithoutError.isNull()).isTrue()
        }

        // remote

        val services2 = createRecordsFactoryWithRemote(services)
        val records2 = services2.recordsServiceV1

        val ref2 = EntityRef.valueOf("remote/$ref")

        val ex2 = assertThrows<Exception> { records2.getAtt(ref2, "errorAtt") }
        assertThat(ex2.message).isEqualTo("ERROR_ATT")

        RequestContext.doWithCtx<Unit>({ it.withOmitErrors(true) }) {
            val valueWithoutError = records2.getAtt(ref2, "errorAtt")
            assertThat(valueWithoutError.isNull()).isTrue()
        }
    }

    @Test
    fun queryErrorTest() {

        val services = RecordsServiceFactory()
        val records = services.recordsServiceV1

        records.register(object : RecordsQueryDao {
            override fun queryRecords(recsQuery: RecordsQuery): Any? {
                error("QUERY_ERROR")
            }
            override fun getId(): String = "query-error-test"
        })

        // local

        val recsQuery = RecordsQuery.create()
            .withSourceId("query-error-test")
            .build()

        val ex = assertThrows<Exception> { records.query(recsQuery) }
        assertThat(ex.message).isEqualTo("QUERY_ERROR")

        RequestContext.doWithCtx<Unit>({ it.withOmitErrors(true) }) {
            val result = records.query(recsQuery)
            assertThat(result.getRecords()).hasSize(0)
        }

        // remote

        val recsQuery2 = RecordsQuery.create()
            .withSourceId("remote/query-error-test")
            .build()

        val services2 = createRecordsFactoryWithRemote(services)
        val records2 = services2.recordsServiceV1

        val ex2 = assertThrows<Exception> { records2.query(recsQuery2) }
        assertThat(ex2.message).isEqualTo("QUERY_ERROR")

        RequestContext.doWithCtx<Unit>({ it.withOmitErrors(true) }) {
            val result = records2.query(recsQuery2)
            assertThat(result.getRecords()).hasSize(0)
        }
    }

    private fun createRecordsFactoryWithRemote(remoteFactory: RecordsServiceFactory): RecordsServiceFactory {

        val restAdapter = remoteFactory.restHandlerAdapter
        val context = EcosWebAppApiMock()
        context.webClientExecuteImpl = { _, path, request ->
            when {
                path.contains("/query") -> {
                    restAdapter.queryRecords(Json.mapper.convert(request, QueryBody::class.java)!!)
                }
                path.contains("/mutate") -> {
                    restAdapter.mutateRecords(Json.mapper.convert(request, MutateBody::class.java)!!)
                }
                path.contains("/delete") -> {
                    restAdapter.deleteRecords(Json.mapper.convert(request, DeleteBody::class.java)!!)
                }
                else -> {
                    error("Unknown path: $path")
                }
            }
        }

        return object : RecordsServiceFactory() {
            override fun getEcosWebAppApi() = context
        }
    }

    class ValueWithErrorAtt {

        fun getErrorAtt(): String {
            error("ERROR_ATT")
        }
    }
}
