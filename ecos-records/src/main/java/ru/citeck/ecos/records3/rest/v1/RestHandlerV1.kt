package ru.citeck.ecos.records3.rest.v1

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.delete.DeleteResp
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateResp
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import ru.citeck.ecos.records3.rest.v1.query.QueryResp
import ru.citeck.ecos.records3.rest.v1.txn.TxnBody
import ru.citeck.ecos.records3.rest.v1.txn.TxnResp
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
    private val recordsTxnService = services.recordsTxnService
    private val recordsResolver = services.recordsResolver
    private val txnActionManager = services.txnActionManager

    private val currentAppId: String
    private val currentAppName: String = services.webappProps.appName
    private val isGateway = services.webappProps.gatewayMode

    init {
        val props = services.webappProps
        var currentAppId = props.appInstanceId.ifBlank {
            props.appName
        }
        if (currentAppId.isBlank()) {
            currentAppId = "unknown:" + UUID.randomUUID()
        }
        this.currentAppId = currentAppId
    }

    fun queryRecords(body: QueryBody, catchError: Boolean = true): QueryResp {
        return doWithContext(body, true) { ctx -> queryRecordsImpl(body, ctx, catchError) }
    }

    private fun queryRecordsImpl(body: QueryBody, context: RequestContext, catchError: Boolean = true): QueryResp {

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
            var result: RecsQueryRes<RecordAtts>
            try {
                result = recordsTxnService.doInTransaction(true) {
                    recordsService.query(query, bodyAttsMap, body.rawAtts)
                }
            } catch (e: Exception) {
                if (catchError) {
                    log.error("Records search query exception. QueryBody: $body", e)
                    result = RecsQueryRes()
                    context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e, services) }
                } else {
                    throw e
                }
            }
            Json.mapper.applyData(resp, result)
        } else {

            // attributes query
            val records = body.getRecords() ?: emptyList()
            if (records.isNotEmpty()) {
                if (body.attributes.isNull()) {
                    resp.setRecords(records.map { RecordAtts(it) })
                } else {
                    var atts: List<RecordAtts>
                    try {
                        atts = recordsTxnService.doInTransaction(true) {
                            recordsService.getAtts(records, bodyAttsMap, body.rawAtts)
                        }
                    } catch (e: Exception) {
                        if (catchError) {
                            log.error("Records attributes query exception. QueryBody: $body", e)
                            atts = records.map { RecordAtts(it) }
                            context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e, services) }
                        } else {
                            throw e
                        }
                    }
                    resp.setRecords(atts)
                }
            }
        }
        resp.setMessages(context.getMessages())
        resp.setRecords(resp.records.map { it.withDefaultAppName(currentAppName) })
        resp.setTxnActions(txnActionManager.getTxnActions(context))
        return resp
    }

    fun mutateRecords(body: MutateBody, catchError: Boolean): MutateResp {
        return doWithContext(body, false) { ctx -> mutateRecordsImpl(body, ctx, catchError) }
    }

    private fun mutateRecordsImpl(body: MutateBody, context: RequestContext, catchError: Boolean): MutateResp {
        if (body.getRecords().isEmpty()) {
            return MutateResp()
        }
        val resp = MutateResp()
        try {
            doInWriteTxn(body.txnId) {
                resp.setRecords(
                    recordsService.mutateAndGetAtts(body.getRecords(), body.attributes, body.rawAtts).map {
                        it.withDefaultAppName(currentAppName)
                    }
                )
                if (body.txnId != null) {
                    resp.txnChangedRecords = context.getTxnChangedRecords() ?: emptySet()
                }
                resp.setTxnActions(txnActionManager.getTxnActions(context))
            }
        } catch (e: Throwable) {
            if (catchError) {
                log.error("Records mutation completed with error. MutateBody: ${body.withoutSensitiveData()}", e)
                resp.setRecords(body.getRecords().map { RecordAtts(it.getId()) })
                context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e, services) }
            } else {
                throw e
            }
        }
        resp.setMessages(context.getMessages())
        return resp
    }

    fun deleteRecords(body: DeleteBody, catchError: Boolean): DeleteResp {
        return doWithContext(body, false) { ctx -> deleteRecordsImpl(body, ctx, catchError) }
    }

    private fun deleteRecordsImpl(body: DeleteBody, context: RequestContext, catchError: Boolean): DeleteResp {
        if (body.records.isEmpty()) {
            return DeleteResp()
        }
        val resp = DeleteResp()
        try {
            doInWriteTxn(body.txnId) {
                resp.setStatuses(recordsService.delete(body.records))
                resp.setTxnActions(txnActionManager.getTxnActions(context))
            }
        } catch (e: Throwable) {
            if (catchError) {
                log.error("Records deletion completed with error. DeleteBody: $body", e)
                resp.setStatuses(body.records.map { DelStatus.ERROR })
                context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e, services) }
            } else {
                throw e
            }
        }
        resp.setMessages(context.getMessages())
        return resp
    }

    fun txnAction(body: TxnBody, catchError: Boolean): TxnResp {
        return doWithContext(body, false) { context ->
            val txnResp = TxnResp()
            try {
                recordsTxnService.doInTransaction(false) {
                    when (body.action) {
                        TxnBody.TxnAction.COMMIT -> recordsResolver.commit(body.records, true)
                        TxnBody.TxnAction.ROLLBACK -> recordsResolver.rollback(body.records, true)
                    }
                }
            } catch (e: Throwable) {
                if (catchError) {
                    log.error("Records txn action completed with error. TxnBody: $body", e)
                    context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e, services) }
                } else {
                    throw e
                }
            }
            txnResp.setMessages(context.getMessages())
            txnResp
        }
    }

    private fun <T> doWithContext(body: RequestBody, readOnly: Boolean, action: (RequestContext) -> T): T {
        return RequestContext.doWithCtx(
            services,
            { ctxData ->
                ctxData.withTxnId(body.txnId)
                ctxData.withReadOnly(readOnly)
                ctxData.withOmitErrors(false)
                ctxData.withRequestId(body.requestId)
                ctxData.withMsgLevel(body.msgLevel)
                val trace: MutableList<String> = ArrayList(body.getRequestTrace())
                trace.add(currentAppId)
                ctxData.withRequestTrace(trace)
                ctxData.withSourceIdMapping(body.sourceIdMapping)
            },
            action
        )
    }

    private inline fun <T> doInWriteTxn(txnId: UUID?, crossinline action: () -> T): T {
        val actionImpl = {
            recordsTxnService.doInTransaction(false) {
                action.invoke()
            }
        }
        if (txnId == null && !isGateway) {
            return RequestContext.doWithTxn(false) { actionImpl.invoke() }
        }
        return actionImpl.invoke()
    }
}
