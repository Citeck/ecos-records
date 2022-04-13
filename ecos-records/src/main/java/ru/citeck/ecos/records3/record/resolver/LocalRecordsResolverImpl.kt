package ru.citeck.ecos.records3.record.resolver

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.DataUriUtil
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordMeta
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.meta.RecordsTemplateService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.OrPredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records2.querylang.QueryWithLang
import ru.citeck.ecos.records2.request.delete.RecordsDeletion
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult
import ru.citeck.ecos.records2.request.mutation.RecordsMutation
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery
import ru.citeck.ecos.records2.source.dao.local.job.Job
import ru.citeck.ecos.records2.source.dao.local.job.JobsProvider
import ru.citeck.ecos.records2.source.info.ColumnsSourceId
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttSchemaResolver
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.records3.record.dao.impl.group.RecordsGroupDao
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceMeta
import ru.citeck.ecos.records3.record.dao.impl.source.client.HasClientMeta
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateWithAnyResDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryResDao
import ru.citeck.ecos.records3.record.dao.query.RecsGroupQueryDao
import ru.citeck.ecos.records3.record.dao.query.SupportsQueryLanguages
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.dao.txn.TxnRecordsDao
import ru.citeck.ecos.records3.record.mixin.AttMixinsHolder
import ru.citeck.ecos.records3.record.mixin.EmptyMixinContext
import ru.citeck.ecos.records3.record.mixin.MixinContext
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.record.resolver.interceptor.*
import ru.citeck.ecos.records3.utils.V1ConvUtils
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.math.max

open class LocalRecordsResolverImpl(private val services: RecordsServiceFactory) : LocalRecordsResolver {

    companion object {

        private val log = KotlinLogging.logger {}

        private const val DEBUG_QUERY_TIME: String = "queryTimeMs"
        private const val DEBUG_REFS_ATTS_TIME: String = "refsAttsTimeMs"
        private const val DEBUG_OBJ_ATTS_TIME: String = "objAttsTimeMs"

        private const val CTX_VAR_QUERY_RAW_PREDICATE: String = "QUERY_RAW_PREDICATE"
    }

    private val allDao = ConcurrentHashMap<String, RecordsDao>()
    private val attsDao = ConcurrentHashMap<String, Pair<RecordsDao, RecordsAttsDao>>()
    private val queryDao = ConcurrentHashMap<String, Pair<RecordsDao, RecordsQueryResDao>>()
    private val mutateDao = ConcurrentHashMap<String, Pair<RecordsDao, RecordsMutateWithAnyResDao>>()
    private val deleteDao = ConcurrentHashMap<String, Pair<RecordsDao, RecordsDeleteDao>>()
    private val txnDao = ConcurrentHashMap<String, Pair<RecordsDao, TxnRecordsDao>>()

    private val daoMapByType = mapOf<Class<*>, Map<String, Pair<RecordsDao, RecordsDao>>>(
        Pair(RecordsAttsDao::class.java, attsDao),
        Pair(RecordsQueryResDao::class.java, queryDao),
        Pair(RecordsMutateWithAnyResDao::class.java, mutateDao),
        Pair(RecordsDeleteDao::class.java, deleteDao),
        Pair(TxnRecordsDao::class.java, txnDao)
    )

    private val queryLangService = services.queryLangService
    private val recordsAttsService = services.recordsAttsService
    private val localRecordsResolverV0 = services.localRecordsResolverV0
    private lateinit var recordsTemplateService: RecordsTemplateService

    private val converter: RecsDaoConverter = RecsDaoConverter()

    private val currentApp = services.properties.appName
    private val jobExecutor = services.jobExecutor

    private var interceptors: List<LocalRecordsInterceptor> = emptyList()

    private var hasDaoWithEmptyId = false

    override fun query(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecsQueryRes<RecordAtts> {
        return if (interceptors.isEmpty()) {
            queryImpl(queryArg, attributes, rawAtts)
        } else {
            QueryInterceptorsChain(this, interceptors.iterator()).invoke(queryArg, attributes, rawAtts)
        }
    }

    internal fun queryImpl(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecsQueryRes<RecordAtts> {

        val context: RequestContext = RequestContext.getCurrentNotNull()

        var query = queryArg
        if (query.language == PredicateService.LANGUAGE_PREDICATE) {

            context.putVar(CTX_VAR_QUERY_RAW_PREDICATE, query.query)

            var queryRes = recordsTemplateService.resolve(query.query, RecordRef.create("meta", ""))
            if (queryRes.isNull()) {
                queryRes = query.query
            }

            val predicate = Json.mapper.convert(queryRes, Predicate::class.java)
            queryRes = DataValue.create(
                PredicateUtils.mapValuePredicates(predicate) { pred ->

                    val type = pred.getType()
                    if (pred.getValue().isArray() &&
                        (
                            type == ValuePredicate.Type.EQ ||
                                type == ValuePredicate.Type.CONTAINS ||
                                type == ValuePredicate.Type.LIKE
                            )
                    ) {

                        val orPredicates = mutableListOf<Predicate>()
                        pred.getValue().forEach { elem ->
                            orPredicates.add(ValuePredicate(pred.getAttribute(), pred.getType(), elem))
                        }

                        OrPredicate.of(orPredicates)
                    } else {
                        pred
                    }
                }
            )
            query = query.copy().withQuery(queryRes).build()
        }

        var sourceId = query.sourceId
        val appDelimIdx = sourceId.indexOf('/')

        if (appDelimIdx != -1) {
            val appName = sourceId.substring(0, appDelimIdx)
            if (appName == currentApp) {
                sourceId = sourceId.substring(appDelimIdx + 1)
                query = query.copy().withSourceId(sourceId).build()
            }
        }

        val finalQuery = query
        var recordsResult: RecsQueryRes<RecordAtts>? = null

        if (query.groupBy.isNotEmpty()) {

            val dao = getRecordsDaoPair(sourceId, RecordsQueryResDao::class.java)

            if (dao == null || dao.first !is RecsGroupQueryDao) {

                val groupsSource = needRecordsDaoPair(RecordsGroupDao.ID, RecordsQueryResDao::class.java)
                val convertedQuery = updateQueryLanguage(query, groupsSource)

                if (convertedQuery == null) {

                    val errorMsg = (
                        "GroupBy is not supported by language: $query" +
                            query.language + ". Query: " + query
                        )
                    context.addMsg(MsgLevel.ERROR) { errorMsg }
                } else {

                    val queryRes: RecsQueryRes<*>? = groupsSource.second.queryRecords(convertedQuery)
                    if (queryRes != null) {

                        val atts: List<RecordAtts> = recordsAttsService.getAtts(
                            queryRes.getRecords(),
                            attributes,
                            rawAtts,
                            EmptyMixinContext
                        )

                        recordsResult = RecsQueryRes(atts)
                        recordsResult.setHasMore(queryRes.getHasMore())
                        recordsResult.setTotalCount(queryRes.getTotalCount())
                    }
                }
            } else {

                recordsResult = queryRecordsFromDao(dao, query, attributes, rawAtts, context)
            }
        } else {

            if (DistinctQuery.LANGUAGE == query.language) {

                val recsQueryDao = getRecordsDaoPair(sourceId, RecordsQueryResDao::class.java)
                val languages = (recsQueryDao?.first as? SupportsQueryLanguages)?.getSupportedLanguages() ?: emptyList()
                if (!languages.contains(DistinctQuery.LANGUAGE)) {
                    val distinctQuery: DistinctQuery = query.getQuery(DistinctQuery::class.java)
                    recordsResult = RecsQueryRes()
                    recordsResult.setRecords(
                        getDistinctValues(
                            sourceId,
                            distinctQuery,
                            finalQuery.page.maxItems,
                            attributes
                        )
                    )
                }
            } else {

                val dao = getRecordsDaoPair(sourceId, RecordsQueryResDao::class.java)

                if (dao == null) {

                    context.addMsg(MsgLevel.DEBUG) { "Legacy Source Dao: '${query.sourceId}'" }
                    val v0Query = V1ConvUtils.recsQueryV1ToV0(query, context)

                    val queryStartMs = System.currentTimeMillis()

                    val records = localRecordsResolverV0.queryRecords(v0Query, attributes, rawAtts)

                    if (context.isMsgEnabled(MsgLevel.DEBUG)) {
                        context.addMsg(
                            MsgLevel.DEBUG,
                            "$DEBUG_QUERY_TIME: '${System.currentTimeMillis() - queryStartMs}'"
                        )
                    }

                    V1ConvUtils.addErrorMessages(records.errors, context)
                    V1ConvUtils.addDebugMessage(records, context)

                    val queryRes = RecsQueryRes<RecordAtts>()
                    queryRes.setRecords(
                        Json.mapper.convert(records.records, Json.mapper.getListType(RecordAtts::class.java))
                    )
                    queryRes.setHasMore(records.hasMore)
                    queryRes.setTotalCount(records.totalCount)

                    return queryRes
                } else {

                    recordsResult = queryRecordsFromDao(dao, query, attributes, rawAtts, context)
                }
            }
        }
        if (recordsResult == null) {
            recordsResult = RecsQueryRes()
        }
        return RecordsUtils.attsWithDefaultApp(recordsResult, currentApp)
    }

    private fun queryRecordsFromDao(
        dao: Pair<RecordsDao, RecordsQueryResDao>,
        extQuery: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        context: RequestContext
    ): RecsQueryRes<RecordAtts> {

        val recordsResult = RecsQueryRes<RecordAtts>()
        val query = updateQueryLanguage(extQuery, dao) ?: error("Query language is not supported. $extQuery")

        val queryStartMs = System.currentTimeMillis()
        val queryRes = dao.second.queryRecords(query)
        context.addMsg(MsgLevel.DEBUG) {
            "$DEBUG_QUERY_TIME: '${System.currentTimeMillis() - queryStartMs}'"
        }

        if (queryRes != null) {
            val objMixins = if (dao.first is AttMixinsHolder) {
                (dao.first as AttMixinsHolder).getMixinContext()
            } else {
                EmptyMixinContext
            }
            val recAtts: List<RecordAtts> = context.doWithVarNotNull(
                AttSchemaResolver.CTX_SOURCE_ID_KEY,
                query.sourceId
            ) {
                getAtts(queryRes.getRecords(), attributes, rawAtts, objMixins)
            }
            recordsResult.setRecords(recAtts)
            recordsResult.setTotalCount(queryRes.getTotalCount())
            recordsResult.setHasMore(queryRes.getHasMore())
        }

        return recordsResult
    }

    private fun getDistinctValues(
        sourceId: String,
        distinctQuery: DistinctQuery,
        maxCountArg: Int,
        schema: List<SchemaAtt>
    ): List<RecordAtts> {
        var maxCount = maxCountArg
        if (maxCount == -1) {
            maxCount = 50
        }
        var recordsQuery = RecordsQuery.create()
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .withSourceId(sourceId)
            .withMaxItems(max(maxCount, 20))
            .build()

        val query: Optional<Any?> = queryLangService.convertLang(
            distinctQuery.query,
            distinctQuery.language,
            PredicateService.LANGUAGE_PREDICATE
        )
        if (!query.isPresent) {
            log.error("Language " + distinctQuery.language + " is not supported by Distinct Query")
            return emptyList()
        }

        val predicate = Json.mapper.convert(query.get(), Predicate::class.java)
        val distinctPredicate = Predicates.or(Predicates.empty(distinctQuery.attribute))
        val fullPredicate = Predicates.and(predicate, Predicates.not(distinctPredicate))
        var found: Int
        var requests = 0
        val distinctValueAlias = "_distinctValue"
        val distinctValueIdAlias = "_distinctValueId"
        val innerSchema = ArrayList(schema)

        innerSchema.add(SchemaAtt.create().withAlias(distinctValueAlias).withName("?str").build())
        innerSchema.add(SchemaAtt.create().withAlias(distinctValueIdAlias).withName("?id").build())

        val distinctAttSchema: List<SchemaAtt> = listOf(
            SchemaAtt.create()
                .withAlias("att")
                .withName(distinctQuery.attribute)
                .withInner(innerSchema)
                .build()
        )
        val distinctAtt: String = distinctQuery.attribute

        val values = HashMap<String, DataValue?>()
        var skipCount = recordsQuery.page.skipCount

        do {
            recordsQuery = recordsQuery.copy()
                .withQuery(fullPredicate)
                .withSkipCount(skipCount)
                .build()
            val queryResult: RecsQueryRes<RecordAtts> = query(recordsQuery, distinctAttSchema, true)

            found = queryResult.getRecords().size

            for (value in queryResult.getRecords()) {

                val att: DataValue = value.getAtt("att")
                val attStr: String = att.get(distinctValueAlias).asText()
                if (att.isNull() || attStr.isEmpty()) {
                    skipCount++
                } else {
                    val replaced: DataValue? = values.put(attStr, att)
                    if (replaced == null) {
                        distinctPredicate.addPredicate(Predicates.eq(distinctAtt, attStr))
                    }
                }
            }
        } while (found > 0 && values.size <= maxCount && ++requests <= maxCount)

        return values.values.filter { it != null && it.isObject() }.mapNotNull { v ->
            if (v == null) {
                null
            } else {
                val atts: ObjectData = v.asObjectData()
                val ref: RecordRef = RecordRef.valueOf(atts.get(distinctValueIdAlias).asText())
                atts.remove(distinctValueAlias)
                atts.remove(distinctValueIdAlias)
                RecordAtts(ref, atts)
            }
        }
    }

    private fun updateQueryLanguage(
        recordsQuery: RecordsQuery,
        dao: Pair<RecordsDao, RecordsQueryResDao>?
    ): RecordsQuery? {

        if (dao == null) {
            return null
        }
        val supportedLanguages = (dao.first as? SupportsQueryLanguages)?.getSupportedLanguages() ?: emptyList()
        if (supportedLanguages.isEmpty()) {
            return recordsQuery
        }
        val queryWithLangOpt: Optional<QueryWithLang?> = queryLangService.convertLang(
            recordsQuery.query,
            recordsQuery.language,
            supportedLanguages
        )
        if (queryWithLangOpt.isPresent) {
            val queryWithLang: QueryWithLang = queryWithLangOpt.get()
            return recordsQuery.copy {
                query = DataValue.create(queryWithLang.query)
                language = queryWithLang.language
            }
        }
        return null
    }

    override fun getAtts(
        records: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {
        if (records.isEmpty()) {
            return emptyList()
        }
        return if (interceptors.isEmpty()) {
            getAttsImpl(records, attributes, rawAtts)
        } else {
            GetAttsInterceptorsChain(this, interceptors.iterator()).invoke(records, attributes, rawAtts)
        }
    }

    internal fun getAttsImpl(
        records: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        return getAtts(records, attributes, rawAtts, EmptyMixinContext)
    }

    private fun getAtts(
        records: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        mixinsForObjects: MixinContext
    ): List<RecordAtts> {

        val context = RequestContext.getCurrentNotNull()

        if (log.isDebugEnabled) {
            log.debug("getMeta start.\nRecords: $records attributes: $attributes")
        }
        val recordObjs = ArrayList<ValWithIdx<Any?>>()
        val recordRefs = ArrayList<ValWithIdx<RecordRef>>()
        for ((idx, rec) in records.withIndex()) {
            if (rec is RecordRef) {
                var ref = rec
                if (ref.appName == currentApp) {
                    ref = ref.removeAppName()
                }
                recordRefs.add(ValWithIdx(ref, idx))
            } else {
                recordObjs.add(ValWithIdx(rec, idx))
            }
        }

        val result = ArrayList<ValWithIdx<RecordAtts>>()

        val refsStartMs = System.currentTimeMillis()
        val refsAtts: List<RecordAtts> = getRefsAttsImpl(recordRefs.map { obj -> obj.value }, attributes, rawAtts)
        if (context.isMsgEnabled(MsgLevel.DEBUG)) {
            context.addMsg(
                MsgLevel.DEBUG,
                "$DEBUG_REFS_ATTS_TIME: '${System.currentTimeMillis() - refsStartMs}'"
            )
        }

        if (refsAtts.size == recordRefs.size) {
            for (i in refsAtts.indices) {
                result.add(ValWithIdx(refsAtts[i], recordRefs[i].idx))
            }
        } else {
            context.addMsg(MsgLevel.ERROR) {
                "Results count doesn't match with " +
                    "requested. refsAtts: " + refsAtts + " recordRefs: " + recordRefs
            }
        }
        val objAttsStartMs = System.currentTimeMillis()
        val atts: List<RecordAtts> = recordsAttsService.getAtts(
            recordObjs.map { it.value },
            attributes,
            rawAtts,
            mixinsForObjects
        )
        if (context.isMsgEnabled(MsgLevel.DEBUG)) {
            context.addMsg(
                MsgLevel.DEBUG,
                "$DEBUG_OBJ_ATTS_TIME: '${System.currentTimeMillis() - objAttsStartMs}'"
            )
        }

        if (atts.size == recordObjs.size) {
            for (i in atts.indices) {
                result.add(ValWithIdx(atts[i], recordObjs[i].idx))
            }
        } else {
            context.addMsg(MsgLevel.ERROR) {
                "Results count doesn't match with " +
                    "requested. atts: " + atts + " recordObjs: " + recordObjs
            }
        }
        if (log.isDebugEnabled) {
            log.debug("getMeta end.\nRecords: $records attributes: $attributes")
        }
        result.sortBy { it.idx }
        return result.map { it.value }
    }

    private fun getRefsAttsImpl(
        records: Collection<RecordRef>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        if (records.isEmpty()) {
            return emptyList()
        }
        val context: RequestContext = RequestContext.getCurrentNotNull()
        if (attributes.isEmpty()) {
            return records.map { id: RecordRef -> RecordAtts(id) }
        }
        val results = ArrayList<ValWithIdx<RecordAtts>>()

        RecordsUtils.groupRefBySource(records).forEach { (sourceId, recs) ->
            results.addAll(getAttsFromSource(sourceId, recs, attributes, rawAtts, context))
        }
        results.sortBy { it.idx }
        return results.map { it.value }
    }

    private fun getAttsFromSource(
        sourceId: String,
        recs: List<ValWithIdx<RecordRef>>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        context: RequestContext
    ): List<ValWithIdx<RecordAtts>> {

        val results = ArrayList<ValWithIdx<RecordAtts>>()

        val recordsDao = getRecordsDaoPair(sourceId, RecordsAttsDao::class.java)

        if (recordsDao == null) {

            val sourceIdRefs: List<RecordRef?> = recs.map { it.value }

            var attsList: List<RecordAtts>? = null

            try {
                val meta = localRecordsResolverV0.getMeta(sourceIdRefs, attributes, rawAtts).also {
                    it.setRecords(it.records)
                }
                V1ConvUtils.addErrorMessages(meta.errors, context)
                V1ConvUtils.addDebugMessage(meta, context)
                attsList = Json.mapper.convert(meta.records, Json.mapper.getListType(RecordAtts::class.java))
            } catch (e: Throwable) {
                if (context.ctxData.omitErrors) {
                    log.error("Local records resolver v0 error. SourceId: '$sourceId' recs: $recs")
                    context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
                } else {
                    throw e
                }
            }
            if (attsList == null) {
                attsList = emptyList()
            }
            if (attsList.size != sourceIdRefs.size) {
                context.addMsg(MsgLevel.ERROR) { "getMetaImpl request failed. SourceId: '$sourceId' Records: $sourceIdRefs" }
                for (ref in recs) {
                    results.add(ref.withValue { RecordAtts(it) })
                }
            } else {
                for (i in attsList.indices) {
                    results.add(ValWithIdx(attsList[i], recs[i].idx))
                }
            }
        } else {
            var recAtts = recordsDao.second.getRecordsAtts(recs.map { it.value.id })
            if (recAtts == null) {
                recAtts = ArrayList<Any>()
                for (ignored in recs) {
                    recAtts.add(EmptyAttValue.INSTANCE)
                }
            } else {
                recAtts = recAtts.map { it ?: EmptyAttValue.INSTANCE }
            }
            if (recAtts.size != recs.size) {
                val finalRecAtts = recAtts
                context.addMsg(MsgLevel.ERROR) {
                    "getRecordAtts should return " +
                        "same amount of values as in argument. " +
                        "SourceId: " + sourceId + "' " +
                        "Expected length: " + recs.size + " " +
                        "Actual length: " + finalRecAtts.size + " " +
                        "Refs: " + recs + " Atts: " + finalRecAtts
                }
                for (ref in recs) {
                    results.add(ref.withValue { RecordAtts(it) })
                }
            } else {
                val mixins = if (recordsDao.first is AttMixinsHolder) {
                    (recordsDao.first as AttMixinsHolder).getMixinContext()
                } else {
                    EmptyMixinContext
                }
                val refs: List<RecordRef> = recs.map { it.value }
                val atts: List<RecordAtts> = recordsAttsService.getAtts(recAtts, attributes, rawAtts, mixins, refs)
                for (i in recs.indices) {
                    results.add(ValWithIdx(atts[i], i))
                }
            }
        }
        return results
    }

    override fun mutate(records: List<RecordAtts>, attsToLoad: List<SchemaAtt>, rawAtts: Boolean): List<RecordAtts> {
        if (records.isEmpty()) {
            return emptyList()
        }
        val preProcessedRecords = records.map {
            val selfAttValue = readSelfAttribute(it.getAtt(RecordConstants.ATT_SELF))
            if (!selfAttValue.isObject()) {
                it
            } else {
                val newAtts = ObjectData.create()
                it.getAttributes().forEach { key, value ->
                    if (key == RecordConstants.ATT_SELF) {
                        selfAttValue.forEach { selfKey, selfValue ->
                            newAtts.set(selfKey, selfValue)
                        }
                    } else {
                        newAtts.set(key, value)
                    }
                }
                RecordAtts(it.getId(), newAtts)
            }
        }
        return if (interceptors.isEmpty()) {
            mutateImpl(preProcessedRecords, attsToLoad, rawAtts)
        } else {
            MutateInterceptorsChain(this, interceptors.iterator())
                .invoke(preProcessedRecords, attsToLoad, rawAtts)
        }
    }

    private fun readSelfAttribute(value: DataValue): DataValue {
        return if (value.isNull()) {
            value
        } else if (value.isArray()) {
            if (value.size() > 0) {
                readSelfAttribute(value.get(0))
            } else {
                DataValue.NULL
            }
        } else if (value.isObject()) {
            val url = value.get("url")
            if (url.isTextual() && url.asText().startsWith(DataUriUtil.DATA_PREFIX)) {
                Json.mapper.read(url.asText(), ObjectData::class.java)?.getData() ?: DataValue.NULL
            } else {
                value
            }
        } else {
            DataValue.NULL
        }
    }

    internal fun mutateImpl(records: List<RecordAtts>, attsToLoad: List<SchemaAtt>, rawAtts: Boolean): List<RecordAtts> {

        val daoResult = ArrayList<RecordAtts>()
        val refsMapping = HashMap<RecordRef, RecordRef>()

        records.forEach { recordArg: RecordAtts ->

            var record = recordArg
            val appName: String = record.getId().appName
            var sourceId: String = record.getId().sourceId

            if (currentApp == appName) {
                val newId: RecordRef = record.getId().removeAppName()
                refsMapping[newId] = record.getId()
                record = RecordAtts(record, newId)
            } else if (appName.isNotBlank()) {
                sourceId = "$appName/$sourceId"
            }

            val dao = getRecordsDaoPair(sourceId, RecordsMutateWithAnyResDao::class.java)

            if (dao == null) {

                val mutation = RecordsMutation()
                mutation.records = listOf(RecordMeta(record))
                val mutateRes: RecordsMutResult = localRecordsResolverV0.mutate(mutation)
                if (mutateRes.records == null || mutateRes.records.isEmpty()) {
                    daoResult.add(record)
                } else {
                    daoResult.add(mutateRes.records[0])
                }
            } else {

                val localId = record.getId().id
                val mutResValues = dao.second.mutateForAnyRes(listOf(LocalRecordAtts(localId, record.getAtts())))
                val mutRes = mutableListOf<RecordAtts>()

                if (attsToLoad.isEmpty()) {
                    mutResValues.forEach {
                        mutRes.add(RecordAtts(recordsAttsService.getId(it, record.getId())))
                    }
                } else {
                    mutRes.addAll(getAtts(mutResValues, attsToLoad, rawAtts))
                }

                mutRes.forEach { atts ->
                    val ref = atts.getId()
                    if (StringUtils.isBlank(ref.sourceId)) {
                        val newRef = if (sourceId.contains('/')) {
                            RecordRef.valueOf("$sourceId@${ref.id}")
                        } else {
                            RecordRef.create(sourceId, ref.id)
                        }
                        daoResult.add(RecordAtts(atts, newRef))
                    } else {
                        daoResult.add(atts)
                    }
                }
            }
        }
        var result: List<RecordAtts> = daoResult
        if (refsMapping.isNotEmpty()) {
            result = daoResult.map { atts ->
                val newRef = refsMapping[atts.getId()]
                if (newRef != null) {
                    RecordAtts(atts, newRef)
                } else {
                    atts
                }
            }
        }
        return result
    }

    override fun delete(records: List<RecordRef>): List<DelStatus> {
        if (records.isEmpty()) {
            return emptyList()
        }
        return if (interceptors.isEmpty()) {
            deleteImpl(records)
        } else {
            DeleteInterceptorsChain(this, interceptors.iterator()).invoke(records)
        }
    }

    internal fun deleteImpl(records: List<RecordRef>): List<DelStatus> {

        val daoResult = ArrayList<DelStatus>()

        records.forEach { recordArg ->

            var record = recordArg
            val appName = recordArg.appName
            var sourceId = recordArg.sourceId

            if (currentApp == appName) {
                record = recordArg.removeAppName()
            } else if (appName.isNotBlank()) {
                sourceId = "$appName/$sourceId"
            }

            val dao = getRecordsDaoPair(sourceId, RecordsDeleteDao::class.java)
            if (dao == null) {
                val deletion = RecordsDeletion()
                deletion.records = listOf(record)
                localRecordsResolverV0.delete(deletion)
                daoResult.add(DelStatus.OK)
            } else {
                val delResult = dao.second.delete(listOf(record.id))
                daoResult.add(delResult[0])
            }
        }
        return daoResult
    }

    override fun isSourceTransactional(sourceId: String): Boolean {
        return txnDao[sourceId]?.second?.isTransactional() == true
    }

    override fun commit(recordRefs: List<RecordRef>) {
        val txnId = RequestContext.getCurrentNotNull().ctxData.txnId ?: return
        RecordsUtils.groupRefBySource(recordRefs).forEach { (sourceId, recordRefs) ->
            val dao = txnDao[sourceId]
            if (dao != null && dao.second.isTransactional()) {
                dao.second.commit(txnId, recordRefs.map { it.value.id })
            }
        }
    }

    override fun rollback(recordRefs: List<RecordRef>) {
        val txnId = RequestContext.getCurrentNotNull().ctxData.txnId ?: return
        RecordsUtils.groupRefBySource(recordRefs).forEach { (sourceId, recordRefs) ->
            val dao = txnDao[sourceId]
            if (dao != null && dao.second.isTransactional()) {
                dao.second.rollback(txnId, recordRefs.map { it.value.id })
            }
        }
    }

    override fun register(sourceId: String, recordsDao: RecordsDao) {

        allDao[sourceId] = recordsDao
        register(sourceId, attsDao, RecordsAttsDao::class.java, recordsDao)
        register(sourceId, queryDao, RecordsQueryResDao::class.java, recordsDao)
        register(sourceId, mutateDao, RecordsMutateWithAnyResDao::class.java, recordsDao)
        register(sourceId, deleteDao, RecordsDeleteDao::class.java, recordsDao)
        register(sourceId, txnDao, TxnRecordsDao::class.java, recordsDao)

        if (sourceId == "") {
            hasDaoWithEmptyId = true
        }

        if (recordsDao is ServiceFactoryAware) {
            (recordsDao as ServiceFactoryAware).setRecordsServiceFactory(services)
        }
        if (recordsDao is JobsProvider) {
            val jobs: List<Job> = (recordsDao as JobsProvider).jobs
            for (job in jobs) {
                jobExecutor.addJob(sourceId, job)
            }
        }
    }

    override fun unregister(sourceId: String) {

        allDao.remove(sourceId)
        attsDao.remove(sourceId)
        queryDao.remove(sourceId)
        mutateDao.remove(sourceId)
        deleteDao.remove(sourceId)
        txnDao.remove(sourceId)

        if (sourceId == "") {
            hasDaoWithEmptyId = false
        }

        jobExecutor.removeJobs(sourceId)
    }

    private fun <T : RecordsDao?> register(
        id: String,
        registry: MutableMap<String, Pair<RecordsDao, T>>,
        type: Class<T>,
        valueArg: RecordsDao
    ) {
        var value = valueArg
        value = converter.convert(value, type)
        if (type.isInstance(value)) {
            @Suppress("UNCHECKED_CAST")
            val dao = value as T
            registry[id] = Pair(valueArg, dao)
        }
    }

    override fun getSourceInfo(sourceId: String): RecordsSourceMeta? {

        val recordsDao = allDao[sourceId] ?: return localRecordsResolverV0.getSourceInfo(sourceId)

        val recordsSourceInfo = RecordsSourceMeta()
        recordsSourceInfo.id = sourceId

        if (recordsDao is SupportsQueryLanguages) {
            recordsSourceInfo.supportedLanguages = recordsDao.getSupportedLanguages()
        }
        recordsSourceInfo.features.query = queryDao[sourceId] != null
        recordsSourceInfo.features.mutate = mutateDao[sourceId] != null
        recordsSourceInfo.features.delete = deleteDao[sourceId] != null
        recordsSourceInfo.features.getAtts = attsDao[sourceId] != null
        recordsSourceInfo.features.transactional = txnDao[sourceId]?.second?.isTransactional() == true

        val columnsSourceId = recordsDao.javaClass.getAnnotation(ColumnsSourceId::class.java)

        if (columnsSourceId != null && StringUtils.isNotBlank(columnsSourceId.value)) {
            recordsSourceInfo.columnsSourceId = columnsSourceId.value
        }
        if (recordsDao is HasClientMeta) {
            recordsSourceInfo.client = recordsDao.getClientMeta()
        }

        return recordsSourceInfo
    }

    override fun getSourcesInfo(): List<RecordsSourceMeta> {
        val result = ArrayList<RecordsSourceMeta>()
        result.addAll(allDao.keys.mapNotNull { getSourceInfo(it) })
        result.addAll(localRecordsResolverV0.sourcesInfo)
        return result
    }

    private fun <T : RecordsDao> getRecordsDaoPair(sourceId: String, type: Class<T>): Pair<RecordsDao, T>? {
        @Suppress("UNCHECKED_CAST")
        return daoMapByType[type]?.get(sourceId) as? Pair<RecordsDao, T>?
    }

    private fun <T : RecordsDao> needRecordsDaoPair(sourceId: String, type: Class<T>): Pair<RecordsDao, T> {
        return getRecordsDaoPair(sourceId, type) ?: error("Records source is not found: $sourceId")
    }

    override fun containsDao(id: String): Boolean {
        return allDao.containsKey(id)
    }

    override fun <T : Any> getRecordsDao(sourceId: String, type: Class<T>): T? {
        val dao = allDao[sourceId] ?: return null
        return if (type.isInstance(dao)) {
            type.cast(dao)
        } else {
            null
        }
    }

    override fun hasDaoWithEmptyId(): Boolean {
        return hasDaoWithEmptyId || localRecordsResolverV0.hasDaoWithEmptyId()
    }

    override fun getInterceptors(): List<LocalRecordsInterceptor> {
        return ArrayList(this.interceptors)
    }

    override fun addInterceptor(interceptor: LocalRecordsInterceptor) {
        addInterceptors(listOf(interceptor))
    }

    override fun addInterceptors(interceptors: List<LocalRecordsInterceptor>) {
        val newInterceptors = ArrayList(this.interceptors)
        newInterceptors.addAll(interceptors)
        setInterceptors(newInterceptors)
    }

    override fun setInterceptors(interceptors: List<LocalRecordsInterceptor>) {
        this.interceptors = interceptors
    }

    fun setRecordsTemplateService(recordsTemplateService: RecordsTemplateService) {
        this.recordsTemplateService = recordsTemplateService
    }
}
