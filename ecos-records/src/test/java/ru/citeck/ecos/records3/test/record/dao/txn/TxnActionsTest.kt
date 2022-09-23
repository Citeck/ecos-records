package ru.citeck.ecos.records3.test.record.dao.txn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.promise.Promises
import ru.citeck.ecos.commons.test.EcosWebAppContextMock
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.records3.txn.ext.TxnActionComponent
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.api.web.EcosWebClient
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class TxnActionsTest {

    companion object {
        const val ACTION_TYPE = "action-type"
        const val MUT_DAO_SOURCE_ID = "mut-dao"
    }

    private var services: MutableMap<String, RecordsServiceFactory> = HashMap()

    @BeforeEach
    fun beforeEach() {
        services.clear()
    }

    @Test
    fun testWithRollback() {

        val (services0, actions0) = createServices("app0")
        val (services1, actions1) = createServices("app1")

        val actionData = TxnActionData("abc")

        val mutate = { source: RecordsServiceFactory, target: RecordsServiceFactory, nonErrorRecords: Int, error: Boolean ->
            val records = mutableListOf<RecordRef>()
            repeat(nonErrorRecords) {
                records.add(RecordRef.create(target.webappProps.appName, MUT_DAO_SOURCE_ID, "123"))
            }
            if (error) {
                records.add(RecordRef.create(target.webappProps.appName, MUT_DAO_SOURCE_ID, "error"))
            }
            source.recordsServiceV1.mutate(records.map { RecordAtts(it, ObjectData.create(actionData)) })
        }

        RequestContext.doWithTxn {
            mutate(services0, services1, 1, false)
        }

        assertThat(actions0).containsExactly(listOf(actionData))
        assertThat(actions1).isEmpty()

        actions0.clear()
        actions1.clear()

        try {
            RequestContext.doWithTxn {
                mutate(services0, services1, 1, true)
            }
        } catch (e: Exception) {
            // expected error
        }

        assertThat(actions0).isEmpty()
        assertThat(actions1).isEmpty()
    }

    @Test
    fun test() {

        val (services0, actions0) = createServices("app0")
        val (services1, actions1) = createServices("app1")

        val testRecordsDao = RecordsDaoBuilder.create("test")
            .addRecord("rec0", ObjectData.create("""{"key":"value"}"""))
            .build()

        services0.recordsServiceV1.register(testRecordsDao)

        // simple test to verify that remote records is accessible
        val ref0 = RecordRef.create("app0", "test", "rec0")
        val testAtt = services1.recordsServiceV1.getAtt(ref0, "key")
        assertThat(testAtt.asText()).isEqualTo("value")

        val actionData = TxnActionData("abc")

        // mutate without transaction. Action should be executed immediately

        val ref1App0 = RecordRef.create("app0", MUT_DAO_SOURCE_ID, "rec1")
        val ref2App0 = RecordRef.create("app0", MUT_DAO_SOURCE_ID, "rec2")
        services1.recordsServiceV1.mutate(ref1App0, ObjectData.create(actionData))

        assertThat(actions0).hasSize(1)
        assertThat(actions1).hasSize(0)
        assertThat(actions0[0]).isEqualTo(listOf(actionData))

        actions0.clear()
        actions1.clear()

        // mutate in transaction. Action should be executed in app1

        RequestContext.doWithTxn(false) {
            services1.recordsServiceV1.mutate(ref1App0, ObjectData.create(actionData))
        }

        assertThat(actions0).hasSize(0)
        assertThat(actions1).hasSize(1)
        assertThat(actions1[0]).isEqualTo(listOf(actionData))

        actions0.clear()
        actions1.clear()

        // Multiple mutations in transaction. Action should be executed in app1

        RequestContext.doWithTxn(false) {
            services1.recordsServiceV1.mutate(ref1App0, ObjectData.create(actionData))
            services1.recordsServiceV1.mutate(ref1App0, ObjectData.create(actionData))
            services1.recordsServiceV1.mutate(ref1App0, ObjectData.create(actionData))
            services1.recordsServiceV1.mutate(
                listOf(ref1App0, ref2App0).map {
                    RecordAtts(it, ObjectData.create(actionData))
                }
            )
        }

        assertThat(actions0).hasSize(0)
        assertThat(actions1).hasSize(4)
        val expectedActions = listOf(
            listOf(actionData),
            listOf(actionData),
            listOf(actionData),
            listOf(actionData, actionData)
        )
        assertThat(actions1).containsExactlyElementsOf(expectedActions)

        actions0.clear()
        actions1.clear()
    }

    private fun createServices(appId: String): AppData {

        val services = object : RecordsServiceFactory() {

            override fun getEcosWebAppContext(): EcosWebAppContext {
                val context = object : EcosWebAppContextMock(appId) {
                    override fun getWebClient(): EcosWebClient {
                        return object : EcosWebClient {
                            override fun <R : Any> execute(
                                targetApp: String,
                                path: String,
                                version: Int,
                                request: Any,
                                respType: Class<R>
                            ): Promise<R> {
                                val services = this@TxnActionsTest.services[targetApp]!!
                                val restHandler = services.restHandlerAdapter
                                val result = AtomicReference<Any>()
                                thread(start = true) {
                                    result.set(
                                        when (path) {
                                            RemoteRecordsResolver.QUERY_PATH -> restHandler.queryRecords(request)
                                            RemoteRecordsResolver.MUTATE_PATH -> restHandler.mutateRecords(request)
                                            RemoteRecordsResolver.DELETE_PATH -> restHandler.deleteRecords(request)
                                            RemoteRecordsResolver.TXN_PATH -> restHandler.txnAction(request)
                                            else -> error("Unknown path: '$path'")
                                        }
                                    )
                                }.join()
                                return Promises.resolve(Json.mapper.convert(result.get(), respType)!!)
                            }
                        }
                    }
                }
                return context
            }
        }

        val actionsReceivedInExecutor = mutableListOf<List<TxnActionData>>()
        val actionExecutor = object : TxnActionComponent<TxnActionData> {
            override fun execute(actions: List<TxnActionData>) {
                actionsReceivedInExecutor.add(actions)
            }
            override fun getType() = ACTION_TYPE
        }
        services.txnActionManager.register(actionExecutor)

        val recordsDaoWithAction = object : RecordMutateDao {
            override fun mutate(record: LocalRecordAtts): String {
                val data = record.attributes.getAs(TxnActionData::class.java)!!
                services.txnActionManager.execute(ACTION_TYPE, data, RequestContext.getCurrent())
                if (record.id == "error") {
                    error("Expected error")
                }
                return record.id
            }

            override fun getId() = MUT_DAO_SOURCE_ID
        }
        services.recordsServiceV1.register(recordsDaoWithAction)

        this.services[appId] = services
        return AppData(services, actionsReceivedInExecutor)
    }

    data class AppData(
        val services: RecordsServiceFactory,
        val actionsReceivedInExecutor: MutableList<List<TxnActionData>>
    )

    data class TxnActionData(
        val field: String
    )
}
