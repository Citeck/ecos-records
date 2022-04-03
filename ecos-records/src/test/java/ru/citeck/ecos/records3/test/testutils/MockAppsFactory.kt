package ru.citeck.ecos.records3.test.testutils

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import ru.citeck.ecos.records3.rest.v1.txn.TxnBody
import kotlin.random.Random

class MockAppsFactory {

    companion object {
        private const val DEFAULT_GATEWAY_APP_NAME = "gateway"
    }

    private val apps = mutableMapOf<String, MockApp>()

    val requests = mutableListOf<MockAppRequest>()

    fun createGatewayApp(defaultApp: String = "alf"): MockApp {
        return createApp(DEFAULT_GATEWAY_APP_NAME, true, defaultApp)
    }

    fun createApp(name: String): MockApp {
        return createApp(name, false)
    }

    private fun createApp(name: String, gatewayMode: Boolean, defaultApp: String = ""): MockApp {

        val defaultCtxAtts = HashMap<String, Any?>()

        val factory = object : RecordsServiceFactory() {
            public override fun createRemoteRecordsResolver(): RemoteRecordsResolver {
                val resolver = RemoteRecordsResolver(
                    this,
                    object : RemoteRecordsRestApi {
                        override fun <T : Any> jsonPost(url: String, request: Any, respType: Class<T>): T {
                            return RequestContext.doWithoutCtx {
                                this@MockAppsFactory.jsonPost(url, request, respType)
                            }
                        }
                    }
                )
                return resolver
            }

            override fun createProperties(): RecordsProperties {
                val props = super.createProperties()
                props.appName = name
                props.appInstanceId = name + ":" + Random.nextInt(0, Int.MAX_VALUE)
                props.gatewayMode = gatewayMode
                props.defaultApp = defaultApp
                return props
            }
        }

        factory.ctxAttsService.register(object : CtxAttsProvider {
            override fun fillContextAtts(attributes: MutableMap<String, Any?>) {
                attributes.putAll(defaultCtxAtts)
            }
        })

        val app = MockApp(name, factory, defaultCtxAtts)
        apps[name] = app
        return app
    }

    private fun <T : Any> jsonPost(url: String, request: Any, resultType: Class<T>): T {
        // convert to json to emulate network
        val requestBytes = mapper.toBytes(request)!!
        val reqObjData = mapper.read(requestBytes, ObjectData::class.java)!!
        val targetAppName = url.substring(1).substringBefore("/")

        requests.add(MockAppRequest(targetAppName, url, reqObjData))

        val response = if (url.contains(RemoteRecordsResolver.QUERY_URL)) {
            val query = mapper.convert(reqObjData, QueryBody::class.java) ?: error("Incorrect QueryBody. Url: $url")
            val mockApp = apps[targetAppName] ?: error("Application doesn't found: $targetAppName")
            mockApp.factory.restHandlerAdapter.queryRecords(query)
        } else if (url.contains(RemoteRecordsResolver.MUTATE_URL)) {
            val query = mapper.convert(reqObjData, MutateBody::class.java) ?: error("Incorrect MutateBody. Url: $url")
            val mockApp = apps[targetAppName] ?: error("Application doesn't found: $targetAppName")
            mockApp.factory.restHandlerAdapter.mutateRecords(query)
        } else if (url.contains(RemoteRecordsResolver.DELETE_URL)) {
            val query = mapper.convert(reqObjData, DeleteBody::class.java) ?: error("Incorrect DeleteBody. Url: $url")
            val mockApp = apps[targetAppName] ?: error("Application doesn't found: $targetAppName")
            mockApp.factory.restHandlerAdapter.deleteRecords(query)
        } else if (url.contains(RemoteRecordsResolver.TXN_URL)) {
            val query = mapper.convert(reqObjData, TxnBody::class.java) ?: error("Incorrect TxnBody. Url: $url")
            val mockApp = apps[targetAppName] ?: error("Application doesn't found: $targetAppName")
            mockApp.factory.restHandlerAdapter.deleteRecords(query)
        } else {
            throw IllegalArgumentException("Unknown URL: $url")
        }
        return mapper.convert(response, resultType) ?: error("Incorrect result. Url: $url")
    }
}
