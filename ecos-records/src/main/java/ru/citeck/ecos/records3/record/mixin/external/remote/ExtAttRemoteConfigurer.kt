package ru.citeck.ecos.records3.record.mixin.external.remote

import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.value.impl.InnerAttValue
import ru.citeck.ecos.records3.record.mixin.external.ExtAttHandlerContext
import ru.citeck.ecos.records3.record.mixin.external.ExtAttMixinConfigurer
import ru.citeck.ecos.records3.record.mixin.external.ExtMixinConfig
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.api.promise.Promises
import ru.citeck.ecos.webapp.api.web.client.EcosWebClientApi
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

class ExtAttRemoteConfigurer(
    private val registry: ExtAttRemoteRegistry,
    private val webClient: EcosWebClientApi
) : ExtAttMixinConfigurer {

    private var currentSettings: ExtMixinConfig? = null

    init {
        registry.onRemoteDataUpdated { currentSettings?.reconfigure() }
    }

    override fun configure(settings: ExtMixinConfig) {
        currentSettings = settings
        for ((appName, attributes) in registry.getRemoteData()) {
            for (data in attributes) {
                settings.addProvidedAtt(data.attribute)
                    .addRequiredAtts(data.requiredAtts.associateBy { it })
                    .setEcosType(data.typeId)
                    .setPriority(data.priority)
                    .setLocal(true)
                    .withRawHandler { ctx, reqAtts, schemaAtt ->
                        handleAttribute(appName, data.typeId, ctx, reqAtts, schemaAtt)
                    }
            }
        }
    }

    private fun handleAttribute(
        appName: String,
        typeId: String,
        context: ExtAttHandlerContext,
        reqAtts: Map<String, Any?>,
        schemaAtt: SchemaAtt
    ): Promise<Any?> {
        val batch = context.computeIfAbsent("batch-$appName") {
            RequestBatch(appName)
        }
        return batch.eval(typeId, reqAtts, schemaAtt)
    }

    private inner class RequestBatch(
        val appName: String
    ) {
        val flushed = AtomicBoolean(true)
        private var attributes = HashMap<String, MutableList<CalculateExtAttsWebExecutor.ReqAttData>>()

        private var future: CompletableFuture<Map<String, List<Map<String, Any?>>>> = CompletableFuture.completedFuture(emptyMap())
        private var promise: Promise<Map<String, List<Map<String, Any?>>>> = Promises.create(future)

        private val lock = ReentrantLock()

        fun eval(typeId: String, reqAtts: Map<String, Any?>, schemaAtt: SchemaAtt): Promise<Any?> {
            lock.lock()
            try {
                val listOfDataToReq = attributes.computeIfAbsent(typeId) { ArrayList() }
                listOfDataToReq.add(
                    CalculateExtAttsWebExecutor.ReqAttData(reqAtts, schemaAtt)
                )
                val reqDataIdx = listOfDataToReq.lastIndex
                if (flushed.compareAndSet(true, false)) {
                    future = CompletableFuture()
                    promise = Promises.create(future) { flush() }
                }
                return promise.then { it[typeId]?.get(reqDataIdx)?.get(schemaAtt.name) }
            } finally {
                lock.unlock()
            }
        }

        private fun flush() {
            lock.lock()
            try {
                if (!flushed.compareAndSet(false, true)) {
                    return
                }
                if (attributes.isEmpty()) {
                    future.complete(emptyMap())
                    return
                }
                val version = webClient.getApiVersion(appName, CalculateExtAttsWebExecutor.PATH, 0)
                val errorMsg = when (version) {
                    EcosWebClientApi.AV_VERSION_NOT_SUPPORTED -> {
                        "Target app '$appName' doesn't support our API version of '${CalculateExtAttsWebExecutor.PATH}'"
                    }

                    EcosWebClientApi.AV_APP_NOT_AVAILABLE -> {
                        "Target app '$appName' is not available"
                    }

                    EcosWebClientApi.AV_PATH_NOT_SUPPORTED -> {
                        "Target app '$appName' doesn't support our API path '${CalculateExtAttsWebExecutor.PATH}'"
                    }

                    else -> ""
                }
                if (errorMsg.isNotEmpty()) {
                    error(
                        errorMsg + ". Remote attributes can't be calculated: ${attributes.entries.map { entry ->
                            entry.key + " " + entry.value.map { it.toLoad.name }
                        }}"
                    )
                }
                val attsForRequest = attributes
                this.attributes = HashMap()

                val requestBody = CalculateExtAttsWebExecutor.ReqBodyDto(
                    attsForRequest.entries.map {
                        CalculateExtAttsWebExecutor.ReqTypeAtts(it.key, it.value)
                    }
                )
                webClient.newRequest()
                    .targetApp(appName)
                    .version(version)
                    .path(CalculateExtAttsWebExecutor.PATH)
                    .body { it.writeDto(requestBody) }
                    .execute {
                        val resp = it.getBodyReader().readDto(
                            CalculateExtAttsWebExecutor.RespBodyDto::class.java
                        )
                        val resultMap = HashMap<String, List<Map<String, Any?>>>()
                        requestBody.attributes.forEachIndexed { typeIdx, reqData ->
                            val respData = resp.results[typeIdx]
                            val typeAtts = ArrayList<Map<String, InnerAttValue>>()
                            reqData.atts.forEachIndexed { idx, attReqData ->
                                typeAtts.add(mapOf(attReqData.toLoad.name to InnerAttValue(respData[idx])))
                            }
                            resultMap[reqData.typeId] = typeAtts
                        }
                        future.complete(resultMap)
                    }.catch {
                        future.completeExceptionally(it)
                    }
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            } finally {
                lock.unlock()
            }
        }
    }
}
