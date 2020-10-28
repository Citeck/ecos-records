package ru.citeck.ecos.records3.record.resolver

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.rest.v1.RequestBody
import ru.citeck.ecos.records3.rest.v1.query.QueryBody
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Stream


@Slf4j
class RemoteRecordsResolver(factory: RecordsServiceFactory?, restApi: RemoteRecordsRestApi?) {
    private val restApi: RemoteRecordsRestApi?
    private var defaultAppName: String? = ""
    private val sourceIdMapping: MutableMap<String?, String?>? = HashMap()
    private val mapper = mapper
    fun query(query: RecordsQuery,
              attributes: MutableMap<String?, *>,
              rawAtts: Boolean): RecsQueryRes<RecordAtts?>? {
        val context: RequestContext = RequestContext.Companion.getCurrentNotNull()
        var sourceId = query.sourceId
        if (sourceId!!.indexOf('/') == -1) {
            sourceId = "$defaultAppName/$sourceId"
        }
        sourceId = sourceIdMapping!!.getOrDefault(sourceId, sourceId)
        val appName: String
        val appQuery = RecordsQuery(query)
        val appDelimIdx = sourceId!!.indexOf("/")
        appName = sourceId.substring(0, appDelimIdx)
        appQuery.sourceId = sourceId.substring(appDelimIdx + 1)
        val queryBody = QueryBody()
        queryBody.query = appQuery
        queryBody.setAttributes(attributes)
        queryBody.isRawAtts = rawAtts
        setContextProps(queryBody, context)
        val queryResp: QueryResp = execQuery(appName, queryBody, context)
        val result: RecsQueryRes<RecordAtts?> = RecsQueryRes<RecordAtts?>()
        result.setRecords(queryResp.getRecords())
        result.setTotalCount(queryResp.getTotalCount())
        result.setHasMore(queryResp.isHasMore())
        return RecordsUtils.attsWithDefaultApp(result, appName)
    }

    private fun execQuery(appName: String?, queryBody: QueryBody?, context: RequestContext?): QueryResp {
        val v0Body = toV0QueryBody(queryBody, context)
        val appResultObj = postRecords(appName, QUERY_URL, v0Body)
        var result: QueryResp? = toQueryAttsRes(appResultObj, context)
        if (result == null) {
            result = QueryResp()
        } else {
            context!!.addAllMsgs(result.getMessages())
        }
        return result
    }

    private fun toQueryAttsRes(body: ObjectNode?, context: RequestContext?): QueryResp? {
        if (body == null || body.isEmpty) {
            return null
        }
        if (body.path("version").asInt(0) == 1) {
            return mapper.convert(body, QueryResp::class.java)
        }
        val v0Result: RecordsMetaQueryResult = mapper.convert(body, RecordsMetaQueryResult::class.java)
            ?: return null
        V1ConvUtils.addErrorMessages(v0Result.getErrors(), context)
        V1ConvUtils.addDebugMessage(v0Result, context)
        val resp = QueryResp()
        resp.setRecords(v0Result.getRecords()
            .stream()
            .map(Function<RecordMeta?, RecordAtts?> { other: RecordMeta? -> RecordAtts(other) })
            .collect(Collectors.toList()))
        resp.setHasMore(v0Result.getHasMore())
        resp.setTotalCount(v0Result.getTotalCount())
        return resp
    }

    private fun toV0QueryBody(body: QueryBody?, context: RequestContext?): ru.citeck.ecos.records2.request.rest.QueryBody? {
        val v0Body = ru.citeck.ecos.records2.request.rest.QueryBody()
        v0Body.setAttributes(body!!.attributes)
        v0Body.records = body.records
        v0Body.v1Body = body
        if (body.query != null) {
            v0Body.query = V1ConvUtils.recsQueryV1ToV0(body.query, context)
        }
        return v0Body
    }

    fun getAtts(records: MutableList<RecordRef?>,
                attributes: MutableMap<String?, *>,
                rawAtts: Boolean): MutableList<RecordAtts?>? {
        val context: RequestContext = RequestContext.Companion.getCurrentNotNull()
        val result: MutableList<ValWithIdx<RecordAtts?>?> = ArrayList<ValWithIdx<RecordAtts?>?>()
        val refsByApp: MutableMap<String?, MutableList<ValWithIdx<RecordRef?>?>?> = RecordsUtils.groupByApp(records)
        refsByApp.forEach(BiConsumer<String?, MutableList<ValWithIdx<RecordRef?>?>?> { app: String?, refs: MutableList<ValWithIdx<RecordRef?>?>? ->
            if (isBlank(app)) {
                app = defaultAppName
            }
            val queryBody = QueryBody()
            queryBody.records = refs!!.stream()
                .map(Function<ValWithIdx<RecordRef?>?, RecordRef?> { obj: ValWithIdx<RecordRef?>? -> obj.getValue() })
                .map(Function<RecordRef?, RecordRef?> { obj: RecordRef? -> obj.removeAppName() })
                .collect(Collectors.toList())
            queryBody.setAttributes(attributes)
            queryBody.isRawAtts = rawAtts
            val queryResp: QueryResp = execQuery(app, queryBody, context)
            if (queryResp.getRecords() == null || queryResp.getRecords().size != refs.size) {
                RemoteRecordsResolver.log.error("Incorrect response: $queryBody\n query: $queryBody")
                for (ref in refs) {
                    result.add(ValWithIdx<RecordAtts?>(RecordAtts(ref.getValue()), ref.getIdx()))
                }
            } else {
                val recsAtts: MutableList<RecordAtts?> = queryResp.getRecords()
                for (i in refs.indices) {
                    val ref: ValWithIdx<RecordRef?>? = refs[i]
                    val atts: RecordAtts? = recsAtts[i]
                    result.add(ValWithIdx<RecordAtts?>(RecordAtts(atts, ref.getValue()), ref.getIdx()))
                }
            }
        })
        result.sort(Comparator.comparingInt(ToIntFunction<ValWithIdx<RecordAtts?>?> { obj: ValWithIdx<RecordAtts?>? -> obj.getIdx() }))
        return result.stream().map(Function<ValWithIdx<RecordAtts?>?, RecordAtts?> { obj: ValWithIdx<RecordAtts?>? -> obj.getValue() }).collect(Collectors.toList())
    }

    fun mutate(records: MutableList<RecordAtts?>): MutableList<RecordRef?>? {
        val context: RequestContext = RequestContext.Companion.getCurrentNotNull()
        val result: MutableList<ValWithIdx<RecordRef?>?> = ArrayList<ValWithIdx<RecordRef?>?>()
        val attsByApp: MutableMap<String?, MutableList<ValWithIdx<RecordAtts?>?>?> = RecordsUtils.groupAttsByApp(records)
        attsByApp.forEach(BiConsumer<String?, MutableList<ValWithIdx<RecordAtts?>?>?> { app: String?, atts: MutableList<ValWithIdx<RecordAtts?>?>? ->
            if (isBlank(app)) {
                app = defaultAppName
            }
            val mutateBody = MutateBody()
            mutateBody.setRecords(atts!!.stream()
                .map(Function<ValWithIdx<RecordAtts?>?, RecordAtts?> { obj: ValWithIdx<RecordAtts?>? -> obj.getValue() })
                .collect(Collectors.toList()))
            setContextProps(mutateBody, context)
            val v0Body = MutationBody()
            if (context.isMsgEnabled(MsgLevel.DEBUG)) {
                v0Body.setDebug(true)
            }
            v0Body.setRecords(mapper.convert(mutateBody.getRecords(), mapper!!.getListType(RecordMeta::class.java)))
            v0Body.setV1Body(mutateBody)
            val mutRespObj = postRecords(app, MUTATE_URL, v0Body)
            val mutateResp: MutateResp? = toMutateResp(mutRespObj, context)
            if (mutateResp != null && mutateResp.getMessages() != null) {
                mutateResp.getMessages().forEach(Consumer<ReqMsg?> { msg: ReqMsg? -> context.addMsg(msg) })
            }
            if (mutateResp == null || mutateResp.getRecords() == null || mutateResp.getRecords().size != atts.size) {
                context.addMsg(MsgLevel.ERROR) { "Incorrect response: $mutateResp\n query: $mutateBody" }
                for (att in atts) {
                    result.add(ValWithIdx<RecordRef?>(att.getValue().getId(), att.getIdx()))
                }
            } else {
                val recsAtts: MutableList<RecordAtts?> = mutateResp.getRecords()
                for (i in atts.indices) {
                    val refAtts: ValWithIdx<RecordAtts?>? = atts[i]
                    val respAtts: RecordAtts? = recsAtts[i]
                    result.add(ValWithIdx<RecordRef?>(respAtts.getId(), refAtts.getIdx()))
                }
            }
        })
        result.sort(Comparator.comparingInt(ToIntFunction<ValWithIdx<RecordRef?>?> { obj: ValWithIdx<RecordRef?>? -> obj.getIdx() }))
        return result.stream().map(Function<ValWithIdx<RecordRef?>?, RecordRef?> { obj: ValWithIdx<RecordRef?>? -> obj.getValue() }).collect(Collectors.toList())
    }

    private fun toMutateResp(body: ObjectNode?, context: RequestContext?): MutateResp? {
        if (body == null || body.isEmpty) {
            return null
        }
        if (body.path("version").asInt(0) == 1) {
            return mapper.convert(body, MutateResp::class.java)
        }
        val v0Result: RecordsMutResult = mapper.convert(body, RecordsMutResult::class.java) ?: return null
        V1ConvUtils.addErrorMessages(v0Result.getErrors(), context)
        V1ConvUtils.addDebugMessage(v0Result, context)
        val resp = MutateResp()
        resp.setRecords(v0Result.getRecords()
            .stream()
            .map(Function<RecordMeta?, RecordAtts?> { other: RecordMeta? -> RecordAtts(other) })
            .collect(Collectors.toList()))
        return resp
    }

    private fun addAppName(meta: RecordAtts?, app: String?): RecordAtts? {
        return RecordAtts(meta, Function<RecordRef?, RecordRef?> { r: RecordRef? -> r.addAppName(app) })
    }

    private fun removeAppName(meta: RecordAtts?): RecordAtts? {
        return RecordAtts(meta, Function<RecordRef?, RecordRef?> { obj: RecordRef? -> obj.removeAppName() })
    }

    fun delete(records: MutableList<RecordRef?>): MutableList<DelStatus?> {
        val context: RequestContext = RequestContext.Companion.getCurrentNotNull()
        val result: MutableList<ValWithIdx<DelStatus?>?> = ArrayList<ValWithIdx<DelStatus?>?>()
        val attsByApp: MutableMap<String?, MutableList<ValWithIdx<RecordRef?>?>?> = RecordsUtils.groupByApp(records)
        attsByApp.forEach(BiConsumer<String?, MutableList<ValWithIdx<RecordRef?>?>?> { app: String?, refs: MutableList<ValWithIdx<RecordRef?>?>? ->
            if (isBlank(app)) {
                app = defaultAppName
            }
            val deleteBody = DeleteBody()
            deleteBody.setRecords(refs!!.stream()
                .map(Function<ValWithIdx<RecordRef?>?, RecordRef?> { obj: ValWithIdx<RecordRef?>? -> obj.getValue() })
                .collect(Collectors.toList()))
            setContextProps(deleteBody, context)
            val v0Body = DeletionBody()
            v0Body.setRecords(deleteBody.getRecords())
            if (context.isMsgEnabled(MsgLevel.DEBUG)) {
                v0Body.setDebug(true)
            }
            v0Body.setV1Body(deleteBody)
            val delRespObj = postRecords(app, DELETE_URL, v0Body)
            val resp: DeleteResp? = toDeleteResp(delRespObj, context)
            if (resp != null && resp.getMessages() != null) {
                resp.getMessages().forEach(Consumer<ReqMsg?> { msg: ReqMsg? -> context.addMsg(msg) })
            }
            val statues: MutableList<DelStatus?>? = toDelStatuses(refs.size, resp, context)
            for (i in refs.indices) {
                val refAtts: ValWithIdx<RecordRef?>? = refs[i]
                val status: DelStatus? = statues!![i]
                result.add(ValWithIdx<DelStatus?>(status, refAtts.getIdx()))
            }
        })
        result.sort(Comparator.comparingInt(ToIntFunction<ValWithIdx<DelStatus?>?> { obj: ValWithIdx<DelStatus?>? -> obj.getIdx() }))
        return result.stream().map(Function<ValWithIdx<DelStatus?>?, DelStatus?> { obj: ValWithIdx<DelStatus?>? -> obj.getValue() }).collect(Collectors.toList())
    }

    private fun toDeleteResp(data: ObjectNode?, context: RequestContext?): DeleteResp? {
        if (data == null || data.size() == 0) {
            return DeleteResp()
        }
        if (data.path("version").asInt(0) == 1) {
            return mapper.convert(data, DeleteResp::class.java)
        }
        val v0Resp: RecordsDelResult = mapper.convert(data, RecordsDelResult::class.java)
            ?: return DeleteResp()
        val records: MutableList<RecordMeta?> = v0Resp.getRecords()
        val resp = DeleteResp()
        resp.setStatuses(records.stream()
            .map(Function<RecordMeta?, DelStatus?> { r: RecordMeta? -> DelStatus.OK })
            .collect(Collectors.toList()))
        V1ConvUtils.addErrorMessages(v0Resp.getErrors(), context)
        V1ConvUtils.addDebugMessage(v0Resp, context)
        return resp
    }

    private fun toDelStatuses(expectedSize: Int, resp: DeleteResp?, context: RequestContext?): MutableList<DelStatus?>? {
        if (resp == null) {
            return getDelStatuses(expectedSize, DelStatus.ERROR)
        }
        if (resp.getStatuses() != null && resp.getStatuses().size == expectedSize) {
            return resp.getStatuses()
        }
        context!!.addMsg(MsgLevel.ERROR) {
            ("Result statues doesn't match request. "
                + "Expected size: " + expectedSize
                + ". Actual response: " + resp)
        }
        return getDelStatuses(expectedSize, DelStatus.ERROR)
    }

    private fun getDelStatuses(size: Int, status: DelStatus?): MutableList<DelStatus?>? {
        return Stream.generate(Supplier<DelStatus?> { status })
            .limit(size.toLong())
            .collect(Collectors.toList())
    }

    private fun setContextProps(body: RequestBody?, ctx: RequestContext?) {
        val ctxData: RequestCtxData<*>? = ctx!!.ctxData
        body!!.msgLevel = ctxData.getMsgLevel()
        body.requestId = ctxData.getRequestId()
        body.requestTrace = ctxData.getRequestTrace()
    }

    private fun postRecords(appName: String?, url: String?, body: Any?): ObjectNode? {
        val appUrl = "/$appName$url"
        return restApi.jsonPost(appUrl, body, ObjectNode::class.java)
    }

    fun getSourceInfo(sourceId: String): RecordsDaoInfo? {
        //todo
        return null
    }

    //todo
    val sourceInfo: MutableList<Any?>
        get() =//todo
            emptyList()

    fun setDefaultAppName(defaultAppName: String?) {
        this.defaultAppName = defaultAppName
    }

    companion object {
        val BASE_URL: String? = "/api/records/"
        val QUERY_URL: String? = BASE_URL + "query"
        val MUTATE_URL: String? = BASE_URL + "mutate"
        val DELETE_URL: String? = BASE_URL + "delete"
    }

    init {
        this.restApi = restApi
        val sourceIdMapping: MutableMap<String?, String?> = factory.getProperties().getSourceIdMapping()
        if (sourceIdMapping != null) {
            this.sourceIdMapping!!.putAll(sourceIdMapping)
        }
    }
}
