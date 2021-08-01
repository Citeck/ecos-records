package ru.citeck.ecos.records3.rest

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.request.rest.DeletionBody
import ru.citeck.ecos.records2.request.rest.MutationBody
import ru.citeck.ecos.records2.request.rest.QueryBody
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.rest.v1.RestHandlerV1
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v1.txn.TxnBody
import ru.citeck.ecos.records3.rest.v1.query.QueryBody as QueryBodyV1

class RestHandlerAdapter(services: RecordsServiceFactory) {

    private val restHandlerV0 = services.restHandler
    private val restHandlerV1 = RestHandlerV1(services)
    private val mapper = Json.mapper

    fun queryRecords(body: Any): Any {

        val bodyWithVersion = getBodyWithVersion(body)

        return when (bodyWithVersion.version) {
            0 -> {
                val v0Body: QueryBody = mapper.convert(bodyWithVersion.body, QueryBody::class.java) ?: QueryBody()
                restHandlerV0.queryRecords(v0Body)
            }
            1 -> {
                val v1Body = mapper.convert(bodyWithVersion.body, QueryBodyV1::class.java) ?: QueryBodyV1()
                restHandlerV1.queryRecords(v1Body)
            }
            else -> {
                error("Unknown body version. Body: $bodyWithVersion")
            }
        }
    }

    fun txnAction(body: Any): Any {
        val txnBody = mapper.convert(body, TxnBody::class.java) ?: TxnBody()
        return restHandlerV1.txnAction(txnBody)
    }

    fun deleteRecords(body: Any): Any {

        val bodyWithVersion = getBodyWithVersion(body)

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
                throw IllegalArgumentException("Unknown body version. Body: $bodyWithVersion")
            }
        }
    }

    fun mutateRecords(body: Any): Any {

        val bodyWithVersion = getBodyWithVersion(body)

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
                error("Unknown body version. Body: $bodyWithVersion")
            }
        }
    }

    private fun getBodyWithVersion(body: Any?): BodyWithVersion {

        val jsonBody: ObjectNode = mapper.convert(body, ObjectNode::class.java)
            ?: error("Incorrect request body. Expected JSON Object, but found: $body")

        var version = jsonBody.path("v")
        if (!version.isNumber) {
            version = jsonBody.path("version")
        }
        if (version.isNumber) {
            return BodyWithVersion(jsonBody, version.asInt())
        }

        val v1Body = jsonBody.path("v1Body")
        return if (v1Body is ObjectNode && v1Body.size() > 0) {
            BodyWithVersion(v1Body, 1)
        } else {
            BodyWithVersion(jsonBody, 0)
        }
    }

    data class BodyWithVersion(
        val body: ObjectNode,
        val version: Int = 0
    )
}
