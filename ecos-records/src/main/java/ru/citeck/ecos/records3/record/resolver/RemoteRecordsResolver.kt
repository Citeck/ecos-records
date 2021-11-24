package ru.citeck.ecos.records3.record.resolver

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import mu.KotlinLogging
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.exception.RecordsException
import ru.citeck.ecos.records2.exception.RemoteRecordsException
import ru.citeck.ecos.records2.request.error.RecordsError
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceMeta
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.record.request.msg.ReqMsg
import ru.citeck.ecos.records3.rest.v1.RequestBody
import ru.citeck.ecos.records3.rest.v1.RequestResp
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.delete.DeleteResp
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateResp
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import ru.citeck.ecos.records3.rest.v1.query.QueryResp
import ru.citeck.ecos.records3.rest.v1.txn.TxnBody
import ru.citeck.ecos.records3.rest.v1.txn.TxnResp
import ru.citeck.ecos.records3.security.HasSensitiveData
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class RemoteRecordsResolver(
    val services: RecordsServiceFactory,
    private val restApi: RemoteRecordsRestApi
) {

    companion object {
        val log = KotlinLogging.logger {}

        const val BASE_URL: String = "/api/records/"
        const val QUERY_URL: String = BASE_URL + "query"
        const val MUTATE_URL: String = BASE_URL + "mutate"
        const val DELETE_URL: String = BASE_URL + "delete"
        const val TXN_URL: String = BASE_URL + "txn"

        private val META_CACHE_TIMEOUT = TimeUnit.MINUTES.toMillis(1)
    }

    private var defaultAppName: String = ""
    private val sourceIdMapping = services.properties.sourceIdMapping
    private lateinit var recordsService: RecordsService
    private val txnActionManager = services.txnActionManager

    private val sourceIdMeta: MutableMap<String, RecSrcMeta> = ConcurrentHashMap()

    fun query(
        query: RecordsQuery,
        attributes: Map<String, *>,
        rawAtts: Boolean
    ): RecsQueryRes<RecordAtts> {

        val context: RequestContext = RequestContext.getCurrentNotNull()
        var sourceId = query.sourceId

        if (sourceId.indexOf('/') == -1) {
            sourceId = "$defaultAppName/$sourceId"
        }
        sourceId = sourceIdMapping.getOrDefault(sourceId, sourceId)
        val appName: String

        val appDelimIdx = sourceId.indexOf("/")
        appName = sourceId.substring(0, appDelimIdx)

        val appQuery = query.copy()
            .withSourceId(sourceId.substring(appDelimIdx + 1))
            .build()

        val queryBody = QueryBody()
        queryBody.query = appQuery
        queryBody.setAttributes(attributes)
        queryBody.rawAtts = rawAtts
        setContextProps(queryBody, context)

        val queryResp: QueryResp = exchangeRemoteRequest(appName, QUERY_URL, queryBody, QueryResp::class, context)
        val result = RecsQueryRes<RecordAtts>()

        result.setRecords(queryResp.records)
        result.setTotalCount(queryResp.totalCount)
        result.setHasMore(queryResp.hasMore)

        return RecordsUtils.attsWithDefaultApp(result, appName)
    }

    fun getAtts(records: List<RecordRef>, attributes: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {

        val context: RequestContext = RequestContext.getCurrentNotNull()
        val result = ArrayList<ValWithIdx<RecordAtts>>()
        val refsByApp = RecordsUtils.groupByApp(records)

        refsByApp.forEach { (appArg, refs) ->

            val app = if (StringUtils.isBlank(appArg)) {
                defaultAppName
            } else {
                appArg
            }

            val queryBody = QueryBody()
            queryBody.setRecords(refs.map { it.value.removeAppName() })
            queryBody.setAttributes(attributes)
            queryBody.rawAtts = rawAtts
            setContextProps(queryBody, context)

            val queryResp = exchangeRemoteRequest(app, QUERY_URL, queryBody, QueryResp::class, context)

            if (queryResp.records.size != refs.size) {
                throw RecordsException(
                    "Incorrect " +
                        "response: ${formatObjForLog(queryResp)} " +
                        "query: ${formatObjForLog(queryBody)}"
                )
            } else {
                val recsAtts: List<RecordAtts> = queryResp.records
                for (i in refs.indices) {
                    val ref: ValWithIdx<RecordRef> = refs[i]
                    val atts: RecordAtts = recsAtts[i]
                    result.add(ValWithIdx(RecordAtts(atts, ref.value), ref.idx))
                }
            }
        }
        result.sortBy { it.idx }
        return result.map { it.value }
    }

    fun mutate(records: List<RecordAtts>, attsToLoad: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {

        val context: RequestContext = RequestContext.getCurrentNotNull()
        val result: MutableList<ValWithIdx<RecordAtts>> = ArrayList()
        val attsByApp: Map<String, MutableList<ValWithIdx<RecordAtts>>> = RecordsUtils.groupAttsByApp(records)

        attsByApp.forEach { (appArg, atts) ->

            val appName = if (StringUtils.isBlank(appArg)) {
                defaultAppName
            } else {
                appArg
            }

            val mutateBody = MutateBody()
            mutateBody.setRecords(atts.map { it.value.withoutAppName() })
            mutateBody.setAttributes(attsToLoad)
            mutateBody.rawAtts = rawAtts

            setContextProps(mutateBody, context)

            val mutateResp: MutateResp = exchangeRemoteRequest(
                appName,
                MUTATE_URL,
                mutateBody,
                MutateResp::class,
                context
            )
            if (mutateResp.records.size != atts.size) {
                throw RecordsException(
                    "Incorrect " +
                        "response: ${formatObjForLog(mutateResp)} " +
                        "query: ${formatObjForLog(mutateBody)}"
                )
            } else {
                val recsAtts = mutateResp.records
                for (i in atts.indices) {
                    val refAtts = atts[i]
                    val newAtts = recsAtts[i].withDefaultAppName(appName)
                    result.add(ValWithIdx(newAtts, refAtts.idx))
                }
            }
        }
        result.sortBy { it.idx }
        return result.map { it.value }
    }

    fun delete(records: List<RecordRef>): List<DelStatus> {

        val context: RequestContext = RequestContext.getCurrentNotNull()
        val result: MutableList<ValWithIdx<DelStatus>> = ArrayList()
        val attsByApp: Map<String, MutableList<ValWithIdx<RecordRef>>> = RecordsUtils.groupByApp(records)

        attsByApp.forEach { (appArg, refs) ->

            val app = if (StringUtils.isBlank(appArg)) {
                defaultAppName
            } else {
                appArg
            }

            val deleteBody = DeleteBody()
            deleteBody.setRecords(refs.map { it.value.removeAppName() })
            setContextProps(deleteBody, context)

            val resp: DeleteResp = exchangeRemoteRequest(app, DELETE_URL, deleteBody, DeleteResp::class, context)

            val statuses = resp.statuses
            if (statuses.size != deleteBody.records.size) {
                throw RecordsException(
                    "Result statues doesn't match request. " +
                        "Expected size: " + deleteBody.records.size +
                        ". Actual response: " + formatObjForLog(resp)
                )
            }
            for (i in refs.indices) {
                val refAtts: ValWithIdx<RecordRef> = refs[i]
                result.add(ValWithIdx(statuses[i], refAtts.idx))
            }
        }
        result.sortBy { it.idx }
        return result.map { it.value }
    }

    fun commit(recordRefs: List<RecordRef>) {
        completeTransaction(recordRefs, TxnBody.TxnAction.COMMIT)
    }

    fun rollback(recordRefs: List<RecordRef>) {
        completeTransaction(recordRefs, TxnBody.TxnAction.ROLLBACK)
    }

    fun isSourceTransactional(sourceId: String): Boolean {

        if (!sourceId.contains("/")) {
            return false
        }
        val appNameAndSourceId = sourceId.split('/', limit = 2)
        val sourceMetaId = appNameAndSourceId[0] + "/src@" + appNameAndSourceId[1]

        return getSourceIdMeta(sourceMetaId).isTransactional
    }

    private fun completeTransaction(recordRefs: List<RecordRef>, action: TxnBody.TxnAction) {

        if (recordRefs.isEmpty()) {
            return
        }

        val context = RequestContext.getCurrentNotNull()

        var txnException: Exception? = null

        RecordsUtils.groupRefBySource(recordRefs).forEach { (sourceId, refs) ->

            val appName = sourceId.substringBefore("/", "")

            if (appName.isNotBlank() && isSourceTransactional(sourceId)) {
                val appRefs = refs.map { it.value }
                try {
                    commitImplInApp(appName, appRefs, action, context)
                } catch (e: Exception) {
                    log.error { "Exception while txn commit '${context.ctxData.txnId}'. Records: $appRefs" }
                    // main transaction already completed, and we should
                    // make as much remote commits or rollbacks as possible
                    if (txnException == null) {
                        txnException = e
                    } else {
                        txnException?.addSuppressed(e)
                    }
                }
            }
        }
        val finalException = txnException
        if (finalException != null) {
            throw finalException
        }
    }

    private fun commitImplInApp(
        appName: String,
        refs: List<RecordRef>,
        action: TxnBody.TxnAction,
        context: RequestContext
    ) {

        val body = TxnBody()
        body.setRecords(refs.map { it.removeAppName() })
        body.setAction(action)
        setContextProps(body, context)

        var exception: Exception? = null
        for (i in 1..4) {
            try {
                exchangeRemoteRequest(appName, TXN_URL, body, TxnResp::class, context)
                if (exception != null) {
                    log.info {
                        "$action request with txn ${body.txnId} app $appName and records ${body.records} " +
                            "was completed successfully after ${i - 1} retry"
                    }
                    exception = null
                }
                break
            } catch (e: Exception) {
                exception = e
                if (i == 4) {
                    break
                }
                val sleepTime = i * 1000L
                log.warn {
                    "$action request with txn ${body.txnId} app $appName and records ${body.records} " +
                        "failed with exception ${e::class.simpleName} " +
                        "msg: ${e.message}. Retry sleep: ${sleepTime}ms"
                }
                Thread.sleep(sleepTime)
            }
        }
        if (exception != null) {
            throw exception
        }
    }

    private fun getSourceIdMeta(sourceMetaId: String): RecSrcMeta {
        val meta = sourceIdMeta.computeIfAbsent(sourceMetaId) { evalSourceIdMeta(it) }
        if (System.currentTimeMillis() - meta.time.toEpochMilli() > META_CACHE_TIMEOUT) {
            sourceIdMeta.remove(sourceMetaId)
        }
        return sourceIdMeta.computeIfAbsent(sourceMetaId) { evalSourceIdMeta(it) }
    }

    private fun evalSourceIdMeta(sourceMetaId: String): RecSrcMeta {
        val time = Instant.now()
        val atts = recordsService.getAtts(RecordRef.valueOf(sourceMetaId), RecSrcMetaAtts::class.java)
        return RecSrcMeta(time, atts.isTransactional ?: false)
    }

    private fun setContextProps(body: RequestBody, ctx: RequestContext) {
        val ctxData = ctx.ctxData
        body.msgLevel = ctxData.msgLevel
        body.requestId = ctxData.requestId
        body.txnId = ctxData.txnId
        body.setRequestTrace(ctxData.requestTrace)
    }

    private fun <T : RequestResp> exchangeRemoteRequest(
        appName: String,
        url: String,
        body: RequestBody,
        respType: KClass<T>,
        context: RequestContext
    ): T {

        val respBody = postRecords(appName, url, body)

        if (respBody == null || respBody.isEmpty) {
            throw RecordsException(
                "Expected ${respType.simpleName} but received empty body. " +
                    "app: $appName " +
                    "url: $url " +
                    "body: ${formatObjForLog(body)}"
            )
        }
        val result = Json.mapper.convert(respBody, respType.java) ?: throw RecordsException(
            "Response body can't be converted to ${respType.simpleName}. " +
                "app: $appName " +
                "url: $url " +
                "body: ${formatObjForLog(respBody)}"
        )
        throwErrorIfRequired(result.messages, context)
        context.addAllMsgs(result.messages)
        txnActionManager.execute(result.txnActions, context)

        return result
    }

    private fun formatObjForLog(obj: Any): String {
        return Json.mapper.toString(
            if (obj is HasSensitiveData<*>) {
                obj.withoutSensitiveData()
            } else {
                obj
            }
        ) ?: "null"
    }

    private fun throwErrorIfRequired(messages: List<ReqMsg>, context: RequestContext) {

        if (context.ctxData.omitErrors) {
            return
        }

        for (idx in messages.size - 1 downTo 0) {
            val msg = messages[idx]
            if (msg.level == MsgLevel.ERROR) {
                val textMessage = when (msg.type) {
                    RecordsError.MSG_TYPE -> msg.msg.getAs(RecordsError::class.java)?.msg ?: msg.msg.asText()
                    else -> msg.msg.asText()
                }
                throw RemoteRecordsException(msg, textMessage)
            }
        }
    }

    private fun postRecords(appName: String, url: String, body: Any): ObjectNode? {
        val appUrl = "/$appName$url"
        return restApi.jsonPost(appUrl, body, ObjectNode::class.java)
    }

    fun getSourceInfo(sourceId: String): RecordsSourceMeta? {
        // todo
        return null
    }

    fun getSourcesInfo(): List<RecordsSourceMeta> = emptyList()

    fun setDefaultAppName(defaultAppName: String) {
        this.defaultAppName = defaultAppName
    }

    fun setRecordsService(recordsService: RecordsService) {
        this.recordsService = recordsService
    }

    private data class RecSrcMeta(
        val time: Instant,
        val isTransactional: Boolean
    )

    data class RecSrcMetaAtts(
        @AttName("features.transactional")
        val isTransactional: Boolean?
    )
}
