package ru.citeck.ecos.records3.test.testutils

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.mixin.external.remote.CalculateExtAttsWebExecutor
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi

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
        val webAppContext = EcosWebAppApiMock(appName = name, gatewayMode = gatewayMode)
        webAppContext.webClientExecuteImpl = { targetApp, path, request ->
            jsonPost(targetApp, path, request)
        }

        val factory = object : RecordsServiceFactory() {

            override fun createProperties(): RecordsProperties {
                return super.createProperties()
                    .withDefaultApp(defaultApp)
            }

            override fun getEcosWebAppApi(): EcosWebAppApi {
                return webAppContext
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

    private fun jsonPost(targetApp: String, path: String, request: Any): Any {
        // convert to json to emulate network
        val requestBytes = mapper.toBytes(request)!!
        val reqObjData = mapper.read(requestBytes, ObjectData::class.java)!!

        requests.add(MockAppRequest(targetApp, path, reqObjData))
        val mockApp = apps[targetApp] ?: error("Application doesn't found: $targetApp")

        val response = if (path.contains(RemoteRecordsResolver.QUERY_PATH)) {
            val query = mapper.convert(reqObjData, ObjectNode::class.java) ?: error("Incorrect QueryBody. Url: $path")
            mockApp.factory.restHandlerAdapter.queryRecords(query, 2)
        } else if (path.contains(RemoteRecordsResolver.MUTATE_PATH)) {
            val query = mapper.convert(reqObjData, ObjectNode::class.java) ?: error("Incorrect MutateBody. Url: $path")
            mockApp.factory.restHandlerAdapter.mutateRecords(query, 1)
        } else if (path.contains(RemoteRecordsResolver.DELETE_PATH)) {
            val query = mapper.convert(reqObjData, ObjectNode::class.java) ?: error("Incorrect DeleteBody. Url: $path")
            mockApp.factory.restHandlerAdapter.deleteRecords(query, 1)
        } else if (path.contains(CalculateExtAttsWebExecutor.PATH)) {

            val req = mapper.convert(
                reqObjData,
                CalculateExtAttsWebExecutor.ReqBodyDto::class.java
            ) ?: error("Incorrect QueryBody. Url: $path")

            return mockApp.calculateExtAttsWebExecutor.execute(req)
        } else {
            throw IllegalArgumentException("Unknown URL: $path")
        }
        return response
    }
}
