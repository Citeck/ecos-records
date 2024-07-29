package ru.citeck.ecos.records3.rest.v1

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.delete.DeleteResp
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateResp
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import ru.citeck.ecos.records3.rest.v1.query.QueryResp
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.MutableList
import kotlin.collections.emptyMap
import kotlin.collections.set

class RestHandlerV1(private val services: RecordsServiceFactory) {

    private val recordsService = services.recordsService

    private val currentAppRef: String
    private val currentAppName: String = services.webappProps.appName

    init {
        val props = services.webappProps
        this.currentAppRef = props.appName + ":" + props.appInstanceId
    }

    fun queryRecords(body: QueryBody): QueryResp {
        return doWithContext(body, true) { ctx -> queryRecordsImpl(body, ctx) }
    }

    private fun queryRecordsImpl(body: QueryBody, context: RequestContext): QueryResp {

        if (body.getQuery() != null && body.getRecords() != null) {
            context.addMsg(MsgLevel.WARN) {
                (
                    "There must be one of 'records' or 'query' field " +
                        "but found both. 'records' field will be ignored"
                    )
            }
        }

        val bodyAtts = body.attributes
        val bodyAttsMap: Map<String, Any?>

        if (bodyAtts.isNull()) {
            bodyAttsMap = emptyMap()
        } else {
            when {
                bodyAtts.isArray() -> {
                    bodyAttsMap = LinkedHashMap()
                    bodyAtts.forEach { att -> bodyAttsMap[att.asText()] = att }
                }
                bodyAtts.isObject() -> {
                    bodyAttsMap = DataValue.create(bodyAtts).asMap(String::class.java, Any::class.java)
                }
                else -> {
                    error("Incorrect attributes: '$bodyAtts'")
                }
            }
        }

        val resp = QueryResp()
        val query = body.getQuery()

        if (query != null) {

            // search query
            val result = recordsService.query(query, bodyAttsMap, body.rawAtts)
            Json.mapper.applyData(resp, result)
        } else {

            // attributes query
            val records = body.getRecords() ?: emptyList()
            if (records.isNotEmpty()) {
                if (body.attributes.isNull()) {
                    resp.setRecords(records.map { RecordAtts(it) })
                } else {
                    resp.setRecords(recordsService.getAtts(records, bodyAttsMap, body.rawAtts))
                }
            }
        }
        resp.setMessages(context.getMessages())
        resp.setRecords(resp.records.map { it.withDefaultAppName(currentAppName) })
        return resp
    }

    fun mutateRecords(body: MutateBody): MutateResp {
        return doWithContext(body, false) { ctx -> mutateRecordsImpl(body, ctx) }
    }

    private fun mutateRecordsImpl(body: MutateBody, context: RequestContext): MutateResp {
        if (body.getRecords().isEmpty()) {
            return MutateResp()
        }
        val resp = MutateResp()
        resp.setRecords(
            recordsService.mutateAndGetAtts(body.getRecords(), body.attributes, body.rawAtts).map {
                it.withDefaultAppName(currentAppName)
            }
        )
        resp.setMessages(context.getMessages())
        return resp
    }

    fun deleteRecords(body: DeleteBody): DeleteResp {
        return doWithContext(body, false) { ctx -> deleteRecordsImpl(body, ctx) }
    }

    private fun deleteRecordsImpl(body: DeleteBody, context: RequestContext): DeleteResp {
        if (body.records.isEmpty()) {
            return DeleteResp()
        }
        val resp = DeleteResp()
        resp.setStatuses(recordsService.delete(body.records))
        resp.setMessages(context.getMessages())
        return resp
    }

    private fun <T> doWithContext(body: RequestBody, readOnly: Boolean, action: (RequestContext) -> T): T {
        return RequestContext.doWithCtx(
            services,
            { ctxData ->
                ctxData.withReadOnly(readOnly)
                ctxData.withOmitErrors(false)
                ctxData.withRequestId(body.requestId)
                ctxData.withMsgLevel(body.msgLevel)
                val trace: MutableList<String> = ArrayList(body.getRequestTrace())
                trace.add(currentAppRef)
                ctxData.withRequestTrace(trace)
                ctxData.withSourceIdMapping(body.sourceIdMapping)
            },
            action
        )
    }
}
