package ru.citeck.ecos.records3.record.resolver

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import mu.KotlinLogging
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordMeta
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.RecordsServiceFactory
import ru.citeck.ecos.records2.request.delete.RecordsDelResult
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult
import ru.citeck.ecos.records2.request.query.typed.RecordsMetaQueryResult
import ru.citeck.ecos.records2.request.rest.DeletionBody
import ru.citeck.ecos.records2.request.rest.MutationBody
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.rest.v1.RequestBody
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody
import ru.citeck.ecos.records3.rest.v1.delete.DeleteResp
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody
import ru.citeck.ecos.records3.rest.v1.mutate.MutateResp
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import ru.citeck.ecos.records3.rest.v1.query.QueryResp
import ru.citeck.ecos.records3.utils.V1ConvUtils
import java.util.*
import ru.citeck.ecos.records2.request.rest.QueryBody as QueryBodyV0

class RemoteRecordsResolver(
    val services: RecordsServiceFactory,
    val restApi: RemoteRecordsRestApi
) {

    companion object {
        val log = KotlinLogging.logger {}

        const val BASE_URL: String = "/api/records/"
        const val QUERY_URL: String = BASE_URL + "query"
        const val MUTATE_URL: String = BASE_URL + "mutate"
        const val DELETE_URL: String = BASE_URL + "delete"
    }

    private var defaultAppName: String = ""
    private val sourceIdMapping = services.properties.sourceIdMapping

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

        val queryResp: QueryResp = execQuery(appName, queryBody, context)
        val result = RecsQueryRes<RecordAtts>()

        result.setRecords(queryResp.records)
        result.setTotalCount(queryResp.totalCount)
        result.setHasMore(queryResp.hasMore)
        return RecordsUtils.attsWithDefaultApp(result, appName)
    }

    private fun execQuery(appName: String, queryBody: QueryBody, context: RequestContext): QueryResp {

        val v0Body = toV0QueryBody(queryBody, context)
        val appResultObj = postRecords(appName, QUERY_URL, v0Body)
        var result: QueryResp? = toQueryAttsRes(appResultObj, context)
        if (result == null) {
            result = QueryResp()
        } else {
            context.addAllMsgs(result.messages)
        }
        return result
    }

    private fun toQueryAttsRes(body: ObjectNode?, context: RequestContext): QueryResp? {

        if (body == null || body.isEmpty) {
            return null
        }
        if (body.path("version").asInt(0) == 1) {
            return Json.mapper.convert(body, QueryResp::class.java)
        }
        val v0Result: RecordsMetaQueryResult =
            Json.mapper.convert(body, RecordsMetaQueryResult::class.java) ?: return null

        V1ConvUtils.addErrorMessages(v0Result.errors, context)
        V1ConvUtils.addDebugMessage(v0Result, context)

        val resp = QueryResp()

        resp.setRecords(v0Result.records.map { RecordAtts(it) })
        resp.hasMore = v0Result.hasMore
        resp.totalCount = v0Result.totalCount
        return resp
    }

    private fun toV0QueryBody(body: QueryBody, context: RequestContext): QueryBodyV0 {

        val v0Body = QueryBodyV0()

        v0Body.setAttributes(body.attributes.asJson())
        v0Body.records = body.getRecords()
        v0Body.v1Body = body

        val query = body.query
        if (query != null) {
            v0Body.query = V1ConvUtils.recsQueryV1ToV0(query, context)
        }

        return v0Body
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
            val queryResp = execQuery(app, queryBody, context)

            if (queryResp.records.size != refs.size) {
                log.error("Incorrect response: $queryBody\n query: $queryBody")
                for (ref in refs) {
                    result.add(ValWithIdx<RecordAtts>(RecordAtts(ref.value), ref.idx))
                }
            } else {
                val recsAtts: List<RecordAtts> = queryResp.records
                for (i in refs.indices) {
                    val ref: ValWithIdx<RecordRef> = refs[i]
                    val atts: RecordAtts = recsAtts[i]
                    result.add(ValWithIdx<RecordAtts>(RecordAtts(atts, ref.value), ref.idx))
                }
            }
        }
        result.sortBy { it.idx }
        return result.map { it.value }
    }

    fun mutate(records: List<RecordAtts>): List<RecordRef> {

        val context: RequestContext = RequestContext.getCurrentNotNull()
        val result: MutableList<ValWithIdx<RecordRef>> = ArrayList<ValWithIdx<RecordRef>>()
        val attsByApp: Map<String, MutableList<ValWithIdx<RecordAtts>>> = RecordsUtils.groupAttsByApp(records)

        attsByApp.forEach { (appArg, atts) ->

            val app = if (StringUtils.isBlank(appArg)) {
                defaultAppName
            } else {
                appArg
            }

            val mutateBody = MutateBody()
            mutateBody.setRecords(atts.map { it.value })

            setContextProps(mutateBody, context)
            val v0Body = MutationBody()
            if (context.isMsgEnabled(MsgLevel.DEBUG)) {
                v0Body.setDebug(true)
            }
            v0Body.records = (
                Json.mapper.convert(
                    mutateBody.getRecords(),
                    Json.mapper.getListType(RecordMeta::class.java)
                )
                )
            v0Body.v1Body = mutateBody

            val mutRespObj = postRecords(app, MUTATE_URL, v0Body)
            val mutateResp: MutateResp? = toMutateResp(mutRespObj, context)

            if (mutateResp?.messages != null) {
                mutateResp.messages.forEach { context.addMsg(it) }
            }
            if (mutateResp?.records == null || mutateResp.records.size != atts.size) {
                context.addMsg(MsgLevel.ERROR) { "Incorrect response: $mutateResp\n query: $mutateBody" }
                for (att in atts) {
                    result.add(ValWithIdx(att.value.getId(), att.idx))
                }
            } else {
                val recsAtts: MutableList<RecordAtts> = mutateResp.records
                for (i in atts.indices) {
                    val refAtts: ValWithIdx<RecordAtts> = atts[i]
                    val respAtts: RecordAtts = recsAtts[i]
                    result.add(ValWithIdx<RecordRef>(respAtts.getId(), refAtts.idx))
                }
            }
        }
        result.sortBy { it.idx }
        return result.map { it.value }
    }

    private fun toMutateResp(body: ObjectNode?, context: RequestContext): MutateResp? {

        if (body == null || body.isEmpty) {
            return null
        }
        if (body.path("version").asInt(0) == 1) {
            return Json.mapper.convert(body, MutateResp::class.java)
        }
        val v0Result = Json.mapper.convert(body, RecordsMutResult::class.java) ?: return null

        V1ConvUtils.addErrorMessages(v0Result.errors, context)
        V1ConvUtils.addDebugMessage(v0Result, context)

        val resp = MutateResp()

        resp.setRecords(v0Result.records.map { RecordAtts(it) })
        return resp
    }

    fun delete(records: List<RecordRef>): List<DelStatus> {

        val context: RequestContext = RequestContext.getCurrentNotNull()
        val result: MutableList<ValWithIdx<DelStatus>> = ArrayList<ValWithIdx<DelStatus>>()
        val attsByApp: Map<String, MutableList<ValWithIdx<RecordRef>>> = RecordsUtils.groupByApp(records)

        attsByApp.forEach { (appArg, refs) ->

            val app = if (StringUtils.isBlank(appArg)) {
                defaultAppName
            } else {
                appArg
            }

            val deleteBody = DeleteBody()
            deleteBody.setRecords(refs.map { it.value })
            setContextProps(deleteBody, context)

            val v0Body = DeletionBody()
            v0Body.records = deleteBody.records
            if (context.isMsgEnabled(MsgLevel.DEBUG)) {
                v0Body.isDebug = true
            }
            v0Body.v1Body = deleteBody
            val delRespObj = postRecords(app, DELETE_URL, v0Body)
            val resp: DeleteResp? = toDeleteResp(delRespObj, context)

            resp?.messages?.forEach { msg -> context.addMsg(msg) }
            val statues = toDelStatuses(refs.size, resp, context)
            for (i in refs.indices) {
                val refAtts: ValWithIdx<RecordRef> = refs[i]
                val status: DelStatus = statues[i]
                result.add(ValWithIdx(status, refAtts.idx))
            }
        }
        result.sortBy { it.idx }
        return result.map { it.value }
    }

    private fun toDeleteResp(data: ObjectNode?, context: RequestContext): DeleteResp? {

        if (data == null || data.size() == 0) {
            return DeleteResp()
        }
        if (data.path("version").asInt(0) == 1) {
            return Json.mapper.convert(data, DeleteResp::class.java)
        }

        val v0Resp = Json.mapper.convert(data, RecordsDelResult::class.java) ?: return DeleteResp()
        val records = v0Resp.records
        val resp = DeleteResp()

        resp.setStatuses(records.map { DelStatus.OK })
        V1ConvUtils.addErrorMessages(v0Resp.errors, context)
        V1ConvUtils.addDebugMessage(v0Resp, context)

        return resp
    }

    private fun toDelStatuses(
        expectedSize: Int,
        resp: DeleteResp?,
        context: RequestContext
    ): List<DelStatus> {
        if (resp == null) {
            return getDelStatuses(expectedSize, DelStatus.ERROR)
        }
        if (resp.statuses.size == expectedSize) {
            return resp.statuses
        }
        context.addMsg(MsgLevel.ERROR) {
            (
                "Result statues doesn't match request. " +
                    "Expected size: " + expectedSize +
                    ". Actual response: " + resp
                )
        }
        return getDelStatuses(expectedSize, DelStatus.ERROR)
    }

    private fun getDelStatuses(size: Int, status: DelStatus): List<DelStatus> {
        return generateSequence { status }.take(size).toList()
    }

    private fun setContextProps(body: RequestBody, ctx: RequestContext) {
        val ctxData = ctx.ctxData
        body.msgLevel = ctxData.msgLevel
        body.requestId = ctxData.requestId
        body.setRequestTrace(ctxData.requestTrace)
    }

    private fun postRecords(appName: String, url: String, body: Any): ObjectNode? {
        val appUrl = "/$appName$url"
        return restApi.jsonPost(appUrl, body, ObjectNode::class.java)
    }

    fun getSourceInfo(sourceId: String): RecordsDaoInfo? {
        // todo
        return null
    }

    fun getSourceInfo(): List<RecordsDaoInfo> = emptyList()

    fun setDefaultAppName(defaultAppName: String) {
        this.defaultAppName = defaultAppName
    }
}
