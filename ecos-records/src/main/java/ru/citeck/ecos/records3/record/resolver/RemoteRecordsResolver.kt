package ru.citeck.ecos.records3.record.resolver

import mu.KotlinLogging
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.exception.RecordsException
import ru.citeck.ecos.records2.exception.RemoteRecordsException
import ru.citeck.ecos.records2.request.error.RecordsError
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.cache.Cache
import ru.citeck.ecos.records3.cache.CacheConfig
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
import ru.citeck.ecos.records3.rest.v2.query.QueryBodyV2
import ru.citeck.ecos.records3.security.HasSensitiveData
import ru.citeck.ecos.webapp.api.web.EcosWebClient
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

class RemoteRecordsResolver(
    val services: RecordsServiceFactory
) {

    companion object {
        val log = KotlinLogging.logger {}

        const val BASE_PATH: String = "/records/"
        const val QUERY_PATH: String = BASE_PATH + "query"
        const val MUTATE_PATH: String = BASE_PATH + "mutate"
        const val DELETE_PATH: String = BASE_PATH + "delete"
        const val TXN_PATH: String = BASE_PATH + "txn"
    }

    private var defaultAppName: String = services.properties.defaultApp
    private val sourceIdMapping = services.properties.sourceIdMapping
    private lateinit var recordsService: RecordsService
    private val txnActionManager = services.txnActionManager

    private val sourceIdMeta: Cache<String, RecSrcMeta>
    private val webClient: EcosWebClient = services.getEcosWebAppContext()?.getWebClient()
        ?: error("EcosWebAppContext or WebClient is null")

    init {
        sourceIdMeta = services.cacheManager.create(
            CacheConfig(
                key = "remote-source-id-meta",
                expireAfterWrite = TimeUnit.MINUTES.toMillis(1),
                maxItems = 200
            ),
            RecSrcMeta(false)
        ) { k -> evalSourceIdMeta(k) }
    }

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

        val queryBody = if (webClient.getApiVersion(appName, QUERY_PATH) >= 2) {
            QueryBodyV2()
        } else {
            QueryBody()
        }
        queryBody.setQuery(appQuery)
        queryBody.setAttributes(attributes)
        queryBody.rawAtts = rawAtts
        setContextProps(queryBody, context)

        val queryResp = exchangeRemoteRequest(appName, QUERY_PATH, queryBody, QueryResp::class, context)

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

            val recsAtts: List<RecordAtts> = getAttsForApp(
                app,
                refs.map { it.value.removeAppName() },
                attributes,
                rawAtts,
                context
            )
            for (i in refs.indices) {
                val ref: ValWithIdx<RecordRef> = refs[i]
                val atts: RecordAtts = recsAtts[i]
                result.add(ValWithIdx(RecordAtts(atts, ref.value), ref.idx))
            }
        }
        result.sortBy { it.idx }
        return result.map { it.value }
    }

    private fun getAttsForApp(
        appName: String,
        records: List<RecordRef>,
        attributes: Map<String, *>,
        rawAtts: Boolean,
        context: RequestContext
    ): List<RecordAtts> {

        val queryBody = if (webClient.getApiVersion(appName, QUERY_PATH) >= 2) {
            QueryBodyV2()
        } else {
            QueryBody()
        }
        queryBody.setRecords(records)
        queryBody.setAttributes(attributes)
        queryBody.rawAtts = rawAtts
        setContextProps(queryBody, context)

        val queryResp = exchangeRemoteRequest(appName, QUERY_PATH, queryBody, QueryResp::class, context)

        if (queryResp.records.size != records.size) {
            throw RecordsException(
                "Incorrect " +
                    "response: ${formatObjForLog(queryResp)} " +
                    "query: ${formatObjForLog(queryBody)}"
            )
        }

        return queryResp.records
    }

    // todo: records grouping by appName performed on RecordsService level and this method should be refactored
    fun mutate(records: List<RecordAtts>, attsToLoad: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {

        val context: RequestContext = RequestContext.getCurrentNotNull()
        val result: MutableList<ValWithIdx<RecordAtts>> = ArrayList()
        var attsByApp: Map<String, List<ValWithIdx<RecordAtts>>> = RecordsUtils.groupAttsByApp(records)

        if (defaultAppName.isNotEmpty() && attsByApp.containsKey(defaultAppName) && attsByApp.containsKey("")) {
            val newDefaultAppRecs = ArrayList(attsByApp[defaultAppName] ?: emptyList())
            newDefaultAppRecs.addAll(attsByApp[""] ?: emptyList())
            newDefaultAppRecs.sortBy { it.idx }
            val newAttsByApp = HashMap(attsByApp)
            newAttsByApp[defaultAppName] = newDefaultAppRecs
            newAttsByApp.remove("")
            attsByApp = newAttsByApp
        }

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
                MUTATE_PATH,
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

            val resp: DeleteResp = exchangeRemoteRequest(app, DELETE_PATH, deleteBody, DeleteResp::class, context)

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

        return sourceIdMeta.get(sourceMetaId).isTransactional
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
                exchangeRemoteRequest(appName, TXN_PATH, body, TxnResp::class, context)
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

    private fun evalSourceIdMeta(sourceMetaId: String): RecSrcMeta {
        val atts = recordsService.getAtts(RecordRef.valueOf(sourceMetaId), RecSrcMetaAtts::class.java)
        return RecSrcMeta(atts.isTransactional ?: false)
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
        requestPath: String,
        body: RequestBody,
        respType: KClass<T>,
        context: RequestContext
    ): T {

        val result = webClient.execute(appName, requestPath, body, respType.java).get()

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

    fun getSourceInfo(sourceId: String): RecordsSourceMeta? {
        // todo
        return null
    }

    fun getSourcesInfo(): List<RecordsSourceMeta> = emptyList()

    fun setRecordsService(recordsService: RecordsService) {
        this.recordsService = recordsService
    }

    private data class RecSrcMeta(
        val isTransactional: Boolean
    )

    data class RecSrcMetaAtts(
        @AttName("features.transactional")
        val isTransactional: Boolean?
    )
}
