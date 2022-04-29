package ru.citeck.ecos.records3.test.testutils

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import ru.citeck.ecos.records3.rest.v1.txn.TxnBody
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext

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
        val webAppContext = MockWebAppContext(name)
        webAppContext.webClientExecuteImpl = { targetApp, path, request ->
            jsonPost(targetApp, path, request)
        }

        val factory = object : RecordsServiceFactory() {

            override fun createProperties(): RecordsProperties {
                val props = super.createProperties()
                props.gatewayMode = gatewayMode
                props.defaultApp = defaultApp
                return props
            }

            override fun getEcosWebAppContext(): EcosWebAppContext {
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

        val response = if (path.contains(RemoteRecordsResolver.QUERY_PATH)) {
            val query = mapper.convert(reqObjData, QueryBody::class.java) ?: error("Incorrect QueryBody. Url: $path")
            val mockApp = apps[targetApp] ?: error("Application doesn't found: $targetApp")
            mockApp.factory.restHandlerAdapter.queryRecords(query)
        } else if (path.contains(RemoteRecordsResolver.MUTATE_PATH)) {
            val query = mapper.convert(reqObjData, MutateBody::class.java) ?: error("Incorrect MutateBody. Url: $path")
            val mockApp = apps[targetApp] ?: error("Application doesn't found: $targetApp")
            mockApp.factory.restHandlerAdapter.mutateRecords(query)
        } else if (path.contains(RemoteRecordsResolver.DELETE_PATH)) {
            val query = mapper.convert(reqObjData, DeleteBody::class.java) ?: error("Incorrect DeleteBody. Url: $path")
            val mockApp = apps[targetApp] ?: error("Application doesn't found: $targetApp")
            mockApp.factory.restHandlerAdapter.deleteRecords(query)
        } else if (path.contains(RemoteRecordsResolver.TXN_PATH)) {
            val query = mapper.convert(reqObjData, TxnBody::class.java) ?: error("Incorrect TxnBody. Url: $path")
            val mockApp = apps[targetApp] ?: error("Application doesn't found: $targetApp")
            mockApp.factory.restHandlerAdapter.deleteRecords(query)
        } else {
            throw IllegalArgumentException("Unknown URL: $path")
        }
        return response
    }
}
