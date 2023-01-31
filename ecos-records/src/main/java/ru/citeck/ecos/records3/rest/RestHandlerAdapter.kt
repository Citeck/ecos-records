package ru.citeck.ecos.records3.rest

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.IntNode
import ecos.com.fasterxml.jackson210.databind.node.NullNode
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.request.rest.DeletionBody
import ru.citeck.ecos.records2.request.rest.MutationBody
import ru.citeck.ecos.records2.request.rest.QueryBody
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver
import ru.citeck.ecos.records3.rest.v1.RestHandlerV1
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v1.txn.TxnBody
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

    private val restHandlerV0 = services.restHandler
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
                        request.getBodyReader().readDto(DataValue::class.java),
                        request.getApiVersion()
                    )
                    response.getBodyWriter().writeDto(result)
                }
                override fun getApiVersion() = 0 to 2
                override fun getPath() = RemoteRecordsResolver.QUERY_PATH
                override fun isReadOnly(): Boolean = true
            }
        )
        executors.register(
            object : EcosWebExecutor {
                override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {
                    val result = mutateRecords(
                        request.getBodyReader().readDto(DataValue::class.java),
                        request.getApiVersion()
                    )
                    response.getBodyWriter().writeDto(result)
                }
                override fun getApiVersion() = 0 to 1
                override fun getPath() = RemoteRecordsResolver.MUTATE_PATH
                override fun isReadOnly(): Boolean = false
            }
        )
        executors.register(
            object : EcosWebExecutor {
                override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {
                    val result = deleteRecords(
                        request.getBodyReader().readDto(DataValue::class.java),
                        request.getApiVersion()
                    )
                    response.getBodyWriter().writeDto(result)
                }
                override fun getApiVersion() = 0 to 1
                override fun getPath() = RemoteRecordsResolver.DELETE_PATH
                override fun isReadOnly(): Boolean = false
            }
        )
        executors.register(
            object : EcosWebExecutor {
                override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {
                    val result = txnAction(
                        request.getBodyReader().readDto(DataValue::class.java)
                    )
                    response.getBodyWriter().writeDto(result)
                }
                override fun getApiVersion() = 0 to 1
                override fun getPath() = RemoteRecordsResolver.TXN_PATH
                override fun isReadOnly(): Boolean = false
            }
        )
    }

    @JvmOverloads
    fun queryRecords(body: Any, version: Int? = null): Any {

        val bodyWithVersion = getBodyWithVersion(body, version)

        return when (bodyWithVersion.version) {
            0 -> {
                val v0Body: QueryBody = mapper.convert(bodyWithVersion.body, QueryBody::class.java) ?: QueryBody()
                restHandlerV0.queryRecords(v0Body)
            }
            1, 2 -> {
                var bodyData = bodyWithVersion.body
                val queryType: Class<out QueryBodyV1> = if (bodyWithVersion.version == 2) {
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
            else -> {
                error(UNKNOWN_BODY_VERSION_MSG + ": " + bodyWithVersion.version)
            }
        }
    }

    fun txnAction(body: Any): Any {
        val txnBody = mapper.convert(body, TxnBody::class.java) ?: TxnBody()
        return restHandlerV1.txnAction(txnBody)
    }

    @JvmOverloads
    fun deleteRecords(body: Any, version: Int? = null): Any {

        val bodyWithVersion = getBodyWithVersion(body, version)

        return when (bodyWithVersion.version) {
            0 -> {
                val v0Body = mapper.convert(bodyWithVersion.body, DeletionBody::class.java) ?: DeletionBody()
                restHandlerV0.deleteRecords(v0Body)
            }
            1 -> {
                val v1Body = mapper.convert(bodyWithVersion.body, DeleteBody::class.java) ?: DeleteBody()
                restHandlerV1.deleteRecords(v1Body)
            }
            else -> {
                throw IllegalArgumentException("$UNKNOWN_BODY_VERSION_MSG. Body: $bodyWithVersion")
            }
        }
    }

    @JvmOverloads
    fun mutateRecords(body: Any, version: Int? = null): Any {

        val bodyWithVersion = getBodyWithVersion(body, version)

        return when (bodyWithVersion.version) {
            0 -> {
                val v0Body = mapper.convert(bodyWithVersion.body, MutationBody::class.java) ?: MutationBody()
                restHandlerV0.mutateRecords(v0Body)
            }
            1 -> {
                val v1Body = mapper.convert(bodyWithVersion.body, MutateBody::class.java) ?: MutateBody()
                restHandlerV1.mutateRecords(v1Body)
            }
            else -> {
                error("$UNKNOWN_BODY_VERSION_MSG. Body: $bodyWithVersion")
            }
        }
    }

    private fun getBodyWithVersion(body: Any?, version: Int? = null): BodyWithVersion {

        val jsonBody: ObjectNode = mapper.convert(body, ObjectNode::class.java)
            ?: error("Incorrect request body. Expected JSON Object, but found: $body")

        var bodyVersion: JsonNode = version?.let { IntNode.valueOf(it) } ?: NullNode.getInstance()
        if (!bodyVersion.isNumber) {
            bodyVersion = jsonBody.path("v")
        }
        if (!bodyVersion.isNumber) {
            bodyVersion = jsonBody.path("version")
        }
        if (bodyVersion.isNumber) {
            return BodyWithVersion(jsonBody, bodyVersion.asInt())
        }

        val v1Body = jsonBody.path("v1Body")
        return if (v1Body is ObjectNode && v1Body.size() > 0) {
            BodyWithVersion(v1Body, 1)
        } else {
            BodyWithVersion(jsonBody, 0)
        }
    }

    fun getV1Handler(): RestHandlerV1 {
        return restHandlerV1
    }

    data class BodyWithVersion(
        val body: ObjectNode,
        val version: Int = 0
    )
}
