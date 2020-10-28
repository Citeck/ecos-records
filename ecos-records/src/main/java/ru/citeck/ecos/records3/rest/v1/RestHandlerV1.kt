package ru.citeck.ecos.records3.rest.v1

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordsServiceFactory
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
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

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val recordsService = services.recordsServiceV1
    private val properties = services.properties
    private var currentAppId: String

    init {
        currentAppId = if (StringUtils.isBlank(properties.appInstanceId)) {
            properties.appName
        } else {
            properties.appInstanceId
        }
        if (StringUtils.isBlank(currentAppId)) {
            currentAppId = "unknown:" + UUID.randomUUID()
        }
    }

    fun queryRecords(body: QueryBody): QueryResp {
        return doWithContext(body) { ctx -> queryRecordsImpl(body, ctx) } ?: QueryResp()
    }

    private fun queryRecordsImpl(body: QueryBody, context: RequestContext): QueryResp {

        if (body.query != null && body.records != null) {
            context.addMsg(MsgLevel.WARN) {
                ("There must be one of 'records' or 'query' field "
                    + "but found both. 'records' field will be ignored")
            }
        }

        val bodyAtts = body.attributes
        val bodyAttsMap: Map<String, Any?>

        if (bodyAtts.isNull) {
            bodyAttsMap = emptyMap()
        } else {
            when {
                bodyAtts.isArray -> {
                    bodyAttsMap = LinkedHashMap()
                    bodyAtts.forEach { att -> bodyAttsMap[att.asText()] = att }
                }
                bodyAtts.isObject -> {
                    bodyAttsMap = DataValue.create(bodyAtts).asMap(String::class.java, Any::class.java)
                }
                else -> {
                    error("Incorrect attributes: '$bodyAtts'")
                }
            }
        }

        val resp = QueryResp()
        val query = body.query

        if (query != null) {

            // search query
            var result: RecsQueryRes<RecordAtts>
            try {
                result = doInTransaction(true) {
                    recordsService.query(query, bodyAttsMap, body.rawAtts)
                }
            } catch (e: Exception) {
                log.error("Records search query exception. QueryBody: $body", e)
                result = RecsQueryRes()
                context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
            }
            Json.mapper.applyData(resp, result)

        } else {

            // attributes query
            val records = body.records ?: emptyList()
            if (records.isNotEmpty()) {
                if (body.attributes.isNull) {
                    resp.setRecords(records.map { RecordAtts(it) })
                } else {
                    var atts: List<RecordAtts>
                    try {
                        atts = doInTransaction(true) {
                            recordsService.getAtts(records, bodyAttsMap, body.rawAtts)
                        }
                    } catch (e: Exception) {
                        log.error("Records attributes query exception. QueryBody: $body", e)
                        atts = records.map { RecordAtts(it) }
                        context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
                    }
                    resp.setRecords(atts)
                }
            }
        }
        resp.setMessages(context.getMessages())
        return resp
    }

    fun mutateRecords(body: MutateBody): MutateResp {
        return doWithContext(body) { ctx -> mutateRecordsImpl(body, ctx) }
    }

    private fun mutateRecordsImpl(body: MutateBody, context: RequestContext): MutateResp {
        if (body.records.isEmpty()) {
            return MutateResp()
        }
        val resp = MutateResp()
        try {
            doInTransaction(false) {
                resp.setRecords(recordsService.mutate(body.records).map { RecordAtts(it) })
            }
        } catch (e: Exception) {
            log.error("Records mutation completed with error. MutateBody: $body", e)
            resp.setRecords(body.records.map { RecordAtts(it.getId()) })
            context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
        }
        resp.setMessages(context.getMessages())
        return resp
    }

    fun deleteRecords(body: DeleteBody): DeleteResp {
        return doWithContext(body) { ctx -> deleteRecordsImpl(body, ctx) }
    }

    private fun deleteRecordsImpl(body: DeleteBody, context: RequestContext): DeleteResp {
        if (body.records.isEmpty()) {
            return DeleteResp()
        }
        val resp = DeleteResp()
        try {
            doInTransaction(false) { resp.setStatuses(recordsService.delete(body.records)) }
        } catch (e: Exception) {
            log.error("Records deletion completed with error. DeleteBody: $body", e)
            resp.setStatuses(body.records.map { DelStatus.ERROR })
            context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
        }
        resp.setMessages(context.getMessages())
        return resp
    }

    private fun <T> doWithContext(body: RequestBody, action: (RequestContext) -> T): T {
        return RequestContext.doWithCtx(services, { ctxData ->
            ctxData.withRequestId(body.requestId)
            ctxData.withMsgLevel(body.msgLevel)
            val trace: MutableList<String> = ArrayList(body.requestTrace)
            trace.add(currentAppId)
            ctxData.withRequestTrace(trace)
        }, action)
    }

    private fun <T> doInTransaction(readOnly: Boolean, action: () -> T) : T {
        return action.invoke()
    }
}
