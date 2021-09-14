package ru.citeck.ecos.records3.test.record.dao.txn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.records3.txn.ext.TxnActionExecutor
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class TxnActionsTest {

    companion object {
        const val ACTION_TYPE = "action-type"
    }

    private var services: MutableMap<String, RecordsServiceFactory> = HashMap()

    @BeforeEach
    fun beforeEach() {
        services.clear()
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
        val recordsDaoWithAction = object : RecordMutateDao {
            override fun mutate(record: LocalRecordAtts): String {
                services0.txnActionManager.execute(ACTION_TYPE, actionData, RequestContext.getCurrent())
                return record.id
            }
            override fun getId() = "mut-dao"
        }
        services0.recordsServiceV1.register(recordsDaoWithAction)

        // mutate without transaction. Action should be executed immediately

        val ref1 = RecordRef.create("app0", "mut-dao", "rec0")
        services1.recordsServiceV1.mutate(ref1, mapOf("abc" to "def"))

        assertThat(actions0).hasSize(1)
        assertThat(actions1).hasSize(0)
        assertThat(actions0[0]).isEqualTo(actionData)

        actions0.clear()
        actions1.clear()

        // mutate in transaction. Action should be executed in app1

        RequestContext.doWithTxn(false) {
            services1.recordsServiceV1.mutate(ref1, mapOf("abc" to "def"))
        }

        assertThat(actions0).hasSize(0)
        assertThat(actions1).hasSize(1)
        assertThat(actions1[0]).isEqualTo(actionData)

        actions0.clear()
        actions1.clear()

        // Multiple mutations in transaction. Action should be executed in app1

        RequestContext.doWithTxn(false) {
            services1.recordsServiceV1.mutate(ref1, mapOf("abc" to "def"))
            services1.recordsServiceV1.mutate(ref1, mapOf("abc" to "def"))
            services1.recordsServiceV1.mutate(ref1, mapOf("abc" to "def"))
        }

        assertThat(actions0).hasSize(0)
        assertThat(actions1).hasSize(3)
        actions1.forEach {
            assertThat(it).isEqualTo(actionData)
        }

        actions0.clear()
        actions1.clear()
    }

    private fun createServices(appId: String): AppData {

        val services = object : RecordsServiceFactory() {
            override fun createProperties(): RecordsProperties {
                val props = RecordsProperties()
                props.appName = appId
                props.appInstanceId = appId + ":" + UUID.randomUUID()
                return props
            }
            override fun createRemoteRecordsResolver(): RemoteRecordsResolver {
                return RemoteRecordsResolver(
                    this,
                    object : RemoteRecordsRestApi {
                        override fun <T : Any> jsonPost(url: String, request: Any, respType: Class<T>): T {
                            val remoteAppName = url.substring(1).substringBefore('/')
                            val services = this@TxnActionsTest.services[remoteAppName]!!
                            val restHandler = services.restHandlerAdapter
                            val urlPrefix = "/$remoteAppName"
                            val result = AtomicReference<Any>()
                            thread(start = true) {
                                result.set(
                                    when (url) {
                                        urlPrefix + RemoteRecordsResolver.QUERY_URL -> restHandler.queryRecords(request)
                                        urlPrefix + RemoteRecordsResolver.MUTATE_URL -> restHandler.mutateRecords(request)
                                        urlPrefix + RemoteRecordsResolver.DELETE_URL -> restHandler.deleteRecords(request)
                                        urlPrefix + RemoteRecordsResolver.TXN_URL -> restHandler.txnAction(request)
                                        else -> error("Unknown url: '$url'")
                                    }
                                )
                            }.join()
                            return Json.mapper.convert(result.get(), respType)!!
                        }
                    }
                )
            }
        }

        val actionsReceivedInExecutor = mutableListOf<TxnActionData>()
        val actionExecutor = object : TxnActionExecutor<TxnActionData> {
            override fun execute(action: TxnActionData) {
                actionsReceivedInExecutor.add(action)
            }
            override fun getType() = ACTION_TYPE
        }

        services.txnActionManager.register(actionExecutor)

        this.services[appId] = services
        return AppData(services, actionsReceivedInExecutor)
    }

    data class AppData(
        val services: RecordsServiceFactory,
        val actionsReceivedInExecutor: MutableList<TxnActionData>
    )

    data class TxnActionData(
        val field: String
    )
}
