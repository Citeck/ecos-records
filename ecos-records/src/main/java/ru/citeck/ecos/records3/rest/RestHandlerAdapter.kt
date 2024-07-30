package ru.citeck.ecos.records3.rest

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.records3.rest.v1.RestHandlerV1
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v2.query.QueryBodyV2
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutor
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorReq
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorResp
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorsApi
import ru.citeck.ecos.records3.rest.v1.query.QueryBody as QueryBodyV1

class RestHandlerAdapter(services: RecordsServiceFactory) {

    companion object {
        const val UNKNOWN_BODY_VERSION_MSG = "Unknown body version"
    }

    private val restHandlerV1 = RestHandlerV1(services)
    private val mapper = Json.mapper

    init {
        registerWebExecutors(services.getEcosWebAppApi()?.getWebExecutorsApi())
    }

    private fun registerWebExecutors(executors: EcosWebExecutorsApi?) {
        executors ?: return
        executors.register(
            object : EcosWebExecutor {
                override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {
                    val result = queryRecords(
                        request.getBodyReader().readDto(ObjectNode::class.java),
                        request.getApiVersion()
                    )
                    response.getBodyWriter().writeDto(result)
                }

                // Note: API version 0 is unsupported. However, the EcosLegacyWebClient
                // does not utilize the WebAPI if version 0 is not provided here.
                // This is a temporary solution until all microservices are upgraded
                // to Spring Boot 3+ and Java 21+.
                override fun getApiVersion() = 0 to 2
                override fun getPath() = RemoteRecordsResolver.QUERY_PATH
                override fun isReadOnly(): Boolean = true
            }
        )
        executors.register(
            object : EcosWebExecutor {
                override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {
                    val result = mutateRecords(
                        request.getBodyReader().readDto(ObjectNode::class.java),
                        request.getApiVersion()
                    )
                    response.getBodyWriter().writeDto(result)
                }

                // Note: API version 0 is unsupported. However, the EcosLegacyWebClient
                // does not utilize the WebAPI if version 0 is not provided here.
                // This is a temporary solution until all microservices are upgraded
                // to Spring Boot 3+ and Java 21+.
                override fun getApiVersion() = 0 to 1
                override fun getPath() = RemoteRecordsResolver.MUTATE_PATH
                override fun isReadOnly(): Boolean = false
            }
        )
        executors.register(
            object : EcosWebExecutor {
                override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {
                    val result = deleteRecords(
                        request.getBodyReader().readDto(ObjectNode::class.java),
                        request.getApiVersion()
                    )
                    response.getBodyWriter().writeDto(result)
                }

                // Note: API version 0 is unsupported. However, the EcosLegacyWebClient
                // does not utilize the WebAPI if version 0 is not provided here.
                // This is a temporary solution until all microservices are upgraded
                // to Spring Boot 3+ and Java 21+.
                override fun getApiVersion() = 0 to 1
                override fun getPath() = RemoteRecordsResolver.DELETE_PATH
                override fun isReadOnly(): Boolean = false
            }
        )
    }

    fun queryRecords(body: ObjectNode, version: Int): Any {

        return when (version) {
            1, 2 -> {
                var bodyData = body
                val queryType: Class<out QueryBodyV1> = if (version == 2) {
                    QueryBodyV2::class.java
                } else {
                    val afterId = bodyData.path("query").path("page").path("afterId")
                    if (afterId.isTextual && afterId.asText().isBlank()) {
                        bodyData = bodyData.deepCopy()
                        (bodyData.path("query").path("page") as? ObjectNode)?.remove("afterId")
                    }
                    QueryBodyV1::class.java
                }
                val v1Body = mapper.convert(bodyData, queryType) ?: QueryBodyV1()
                restHandlerV1.queryRecords(v1Body)
            }
            else -> error("$UNKNOWN_BODY_VERSION_MSG: $version")
        }
    }

    fun deleteRecords(body: ObjectNode, version: Int): Any {

        return when (version) {
            1 -> {
                val v1Body = mapper.convert(body, DeleteBody::class.java) ?: DeleteBody()
                restHandlerV1.deleteRecords(v1Body)
            }
            else -> error("$UNKNOWN_BODY_VERSION_MSG: $version")
        }
    }

    fun mutateRecords(body: ObjectNode, version: Int): Any {

        return when (version) {
            1 -> {
                val v1Body = mapper.convert(body, MutateBody::class.java) ?: MutateBody()
                restHandlerV1.mutateRecords(v1Body)
            }
            else -> error("$UNKNOWN_BODY_VERSION_MSG: $version")
        }
    }

    fun getV1Handler(): RestHandlerV1 {
        return restHandlerV1
    }
}
