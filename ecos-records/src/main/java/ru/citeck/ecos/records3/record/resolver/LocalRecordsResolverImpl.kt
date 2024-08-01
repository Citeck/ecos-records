package ru.citeck.ecos.records3.record.resolver

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.DataUriUtil
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.meta.RecordsTemplateService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.OrPredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records2.querylang.QueryWithLang
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery
import ru.citeck.ecos.records2.source.dao.local.job.Job
import ru.citeck.ecos.records2.source.dao.local.job.JobsProvider
import ru.citeck.ecos.records2.source.info.ColumnsSourceId
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.exception.LanguageNotSupportedException
import ru.citeck.ecos.records3.exception.RecordsSourceNotFoundException
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
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateWithAnyResDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryResDao
import ru.citeck.ecos.records3.record.dao.query.RecsGroupQueryDao
import ru.citeck.ecos.records3.record.dao.query.SupportsQueryLanguages
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.mixin.AttMixinsHolder
import ru.citeck.ecos.records3.record.mixin.EmptyMixinContext
import ru.citeck.ecos.records3.record.mixin.MixinContext
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.record.resolver.interceptor.*
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

open class LocalRecordsResolverImpl(private val services: RecordsServiceFactory) :
    LocalRecordsResolver, ServiceFactoryAware {

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
    private val mutateDao = ConcurrentHashMap<String, Pair<RecordsDao, RecordMutateWithAnyResDao>>()
    private val deleteDao = ConcurrentHashMap<String, Pair<RecordsDao, RecordsDeleteDao>>()

    private val daoMapByType = mapOf<Class<*>, Map<String, Pair<RecordsDao, RecordsDao>>>(
        Pair(RecordsAttsDao::class.java, attsDao),
        Pair(RecordsQueryResDao::class.java, queryDao),
        Pair(RecordMutateWithAnyResDao::class.java, mutateDao),
        Pair(RecordsDeleteDao::class.java, deleteDao)
    )

    private val queryLangService = services.queryLangService
    private val recordsAttsService = services.recordsAttsService
    private lateinit var recordsTemplateService: RecordsTemplateService

    private val queryAutoGroupingEnabled = services.properties.queryAutoGroupingEnabled
    private val currentApp = services.webappProps.appName
    private val currentAppRef = currentApp + ":" + services.webappProps.appInstanceId
    private val converter: RecsDaoConverter = RecsDaoConverter(currentApp)

    private val jobExecutor = services.jobExecutor

    private var interceptors: List<LocalRecordsInterceptor> = emptyList()

    private var hasDaoWithEmptyId = false

    override fun queryRecords(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecsQueryRes<RecordAtts> {
        return recordsAttsService.doWithSchema(attributes, rawAtts) { schema ->
            if (interceptors.isEmpty()) {
                queryImpl(queryArg, schema, rawAtts)
            } else {
                QueryRecordsInterceptorsChain(this, interceptors.iterator()).invoke(queryArg, schema, rawAtts)
            }
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

            var queryRes = recordsTemplateService.resolve(query.query, EntityRef.create("meta", ""))
            if (queryRes.isNull()) {
                queryRes = query.query
            }

            val predicate = Json.mapper.convert(queryRes, Predicate::class.java)
            val mappedPredicate = PredicateUtils.mapValuePredicates(
                predicate,
                { pred ->

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
                },
                onlyAnd = false,
                optimize = true,
                filterEmptyComposite = false
            )
            if (mappedPredicate != null) {
                query = query.copy().withQuery(DataValue.createAsIs(mappedPredicate)).build()
            }
        }

        var sourceId = query.sourceId
        val appDelimIdx = sourceId.indexOf('/')

        if (appDelimIdx != -1) {
            val appName = sourceId.substring(0, appDelimIdx)
            if (appName == currentApp || appName == currentAppRef) {
                sourceId = sourceId.substring(appDelimIdx + 1)
                query = query.copy().withSourceId(sourceId).build()
            }
        }

        val finalQuery = query
        var recordsResult: RecsQueryRes<RecordAtts>? = null

        if (query.groupBy.isNotEmpty()) {

            val dao = getRecordsDaoPair(sourceId, RecordsQueryResDao::class.java)

            if (dao == null || dao.first !is RecsGroupQueryDao) {

                if (!queryAutoGroupingEnabled) {
                    return RecsQueryRes()
                }

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
                recordsResult = queryRecordsFromDao(sourceId, dao, query, attributes, rawAtts, context)
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
                if (dao != null) {
                    recordsResult = queryRecordsFromDao(sourceId, dao, query, attributes, rawAtts, context)
                }
            }
        }
        if (recordsResult == null) {
            recordsResult = RecsQueryRes()
        }
        return RecordsUtils.attsWithDefaultApp(recordsResult, currentApp)
    }

    private fun queryRecordsFromDao(
        sourceId: String,
        dao: Pair<RecordsDao, RecordsQueryResDao>,
        extQuery: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        context: RequestContext
    ): RecsQueryRes<RecordAtts> {

        val recordsResult = RecsQueryRes<RecordAtts>()
        val query = updateQueryLanguage(extQuery, dao)
            ?: throw LanguageNotSupportedException(sourceId, extQuery.language)

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
                getAttsFromQueryRes(dao.second.getId(), queryRes.getRecords(), objMixins, attributes, rawAtts, context)
            }
            recordsResult.setRecords(recAtts)
            recordsResult.setTotalCount(queryRes.getTotalCount())
            recordsResult.setHasMore(queryRes.getHasMore())
        }

        return recordsResult
    }

    private fun getAttsFromQueryRes(
        sourceId: String,
        records: List<Any?>,
        queryDaoMixins: MixinContext,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        context: RequestContext
    ): List<RecordAtts> {

        if (records.isEmpty()) {
            return emptyList()
        }

        val currentAppRecIdsBySrcId = mutableMapOf<String, MutableList<ValWithIdx<String>>>()
        val recordRefs = Array(records.size) { EntityRef.EMPTY }

        for ((idx, record) in records.withIndex()) {
            if (record is EntityRef) {
                val appName = record.getAppName()
                if (appName.isBlank() || appName == currentApp) {
                    val srcId = record.getSourceId().ifBlank { sourceId }
                    currentAppRecIdsBySrcId.computeIfAbsent(srcId) { ArrayList() }.add(
                        ValWithIdx(record.getLocalId(), idx)
                    )
                }
                recordRefs[idx] = record
            }
        }
        if (currentAppRecIdsBySrcId.isNotEmpty()) {
            val resolvedAtts = ArrayList<ValWithIdx<RecordAtts>>()
            val alreadyProcessedIndexes = HashSet<Int>()
            for ((srcId, recordIds) in currentAppRecIdsBySrcId) {
                val recordsDao = getRecordsDaoPair(srcId, RecordsAttsDao::class.java) ?: continue
                if (recordIds.size == records.size) {
                    // all records from single local source
                    return getAttsFromRecordsDao(
                        srcId,
                        recordsDao,
                        recordIds.map { it.value },
                        attributes,
                        rawAtts,
                        context
                    )
                } else {
                    getAttsFromRecordsDao(
                        srcId,
                        recordsDao,
                        recordIds.map { it.value },
                        attributes,
                        rawAtts,
                        context
                    ).forEachIndexed { idx, atts ->
                        val recIndex = recordIds[idx].idx
                        alreadyProcessedIndexes.add(recIndex)
                        resolvedAtts.add(ValWithIdx(atts, recIndex))
                    }
                }
            }
            if (alreadyProcessedIndexes.isEmpty()) {
                return getValueAttsWithContextImpl(
                    records,
                    attributes,
                    rawAtts,
                    context,
                    queryDaoMixins,
                    recordRefs.toList()
                )
            } else if (alreadyProcessedIndexes.size == records.size) {
                resolvedAtts.sortBy { it.idx }
                return resolvedAtts.map { it.value }
            } else {

                val recsToResolveAttsSize = records.size - alreadyProcessedIndexes.size
                val recsToResolveAtts = ArrayList<Any?>(recsToResolveAttsSize)
                val recsToResolveAttsIdxs = ArrayList<Int>(recsToResolveAttsSize)
                val recsToResolveAttsRefs = ArrayList<EntityRef>(recsToResolveAttsSize)
                for ((idx, record) in records.withIndex()) {
                    if (!alreadyProcessedIndexes.contains(idx)) {
                        recsToResolveAtts.add(record)
                        recsToResolveAttsIdxs.add(idx)
                        recsToResolveAttsRefs.add(recordRefs[idx])
                    }
                }
                getValueAttsWithContextImpl(
                    recsToResolveAtts,
                    attributes,
                    rawAtts,
                    context,
                    queryDaoMixins,
                    recsToResolveAttsRefs
                ).forEachIndexed { idx, atts ->
                    resolvedAtts.add(ValWithIdx(atts, recsToResolveAttsIdxs[idx]))
                }
                resolvedAtts.sortBy { it.idx }
                return resolvedAtts.map { it.value }
            }
        } else {
            return getValueAttsWithContextImpl(
                records,
                attributes,
                rawAtts,
                context,
                queryDaoMixins,
                null
            )
        }
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
            val queryResult: RecsQueryRes<RecordAtts> = queryRecords(recordsQuery, distinctAttSchema, true)

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
                val ref: EntityRef = EntityRef.valueOf(atts[distinctValueIdAlias].asText())
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

    override fun getValuesAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {
        return recordsAttsService.doWithSchema(attributes, rawAtts) { schema ->
            if (interceptors.isEmpty()) {
                getValueAttsImpl(values, attributes, rawAtts)
            } else {
                GetValuesAttsInterceptorsChain(this, interceptors.iterator())
                    .invoke(values, schema, rawAtts)
            }
        }
    }

    internal fun getValueAttsImpl(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {
        return getValueAttsWithContextImpl(
            values,
            attributes,
            rawAtts,
            RequestContext.getCurrentNotNull(),
            EmptyMixinContext
        )
    }

    private fun getValueAttsWithContextImpl(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        context: RequestContext,
        mixins: MixinContext,
        recordRefs: List<EntityRef>? = null
    ): List<RecordAtts> {

        val objAttsStartMs = System.currentTimeMillis()
        val atts: List<RecordAtts> = recordsAttsService.getAtts(
            values,
            attributes,
            rawAtts,
            mixins,
            recordRefs ?: emptyList()
        )
        if (context.isMsgEnabled(MsgLevel.DEBUG)) {
            context.addMsg(
                MsgLevel.DEBUG,
                "$DEBUG_OBJ_ATTS_TIME: '${System.currentTimeMillis() - objAttsStartMs}'"
            )
        }

        return if (atts.size == values.size) {
            atts
        } else {
            context.addMsg(MsgLevel.ERROR) {
                "Results count doesn't match with " +
                    "requested. atts: " + atts + " values: " + values
            }
            values.map { RecordAtts(EntityRef.EMPTY) }
        }
    }

    override fun getRecordsAtts(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {
        if (recordIds.isEmpty()) {
            return emptyList()
        }
        return recordsAttsService.doWithSchema(attributes, rawAtts) { schema ->
            if (interceptors.isEmpty()) {
                getRecordsAttsImpl(sourceId, recordIds, schema, rawAtts)
            } else {
                GetRecordsAttsInterceptorsChain(this, interceptors.iterator())
                    .invoke(sourceId, recordIds, schema, rawAtts)
            }
        }
    }

    internal fun getRecordsAttsImpl(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        val context = RequestContext.getCurrentNotNull()

        log.trace { "getMeta start.\nSourceId: '$sourceId' Records: $recordIds attributes: $attributes" }

        val refsStartMs = System.currentTimeMillis()
        val refsAtts: List<RecordAtts> = getRefsAttsImpl(sourceId, recordIds, attributes, rawAtts)
        if (context.isMsgEnabled(MsgLevel.DEBUG)) {
            context.addMsg(
                MsgLevel.DEBUG,
                "$DEBUG_REFS_ATTS_TIME: '${System.currentTimeMillis() - refsStartMs}'"
            )
        }

        if (refsAtts.size != recordIds.size) {
            context.addMsg(MsgLevel.ERROR) {
                "Results count doesn't match with " +
                    "requested. refsAtts: " + refsAtts + " records: " + recordIds
            }
        }

        log.trace { "getMeta end.\nSourceId: '$sourceId' Records: $recordIds attributes: $attributes" }

        return refsAtts
    }

    private fun getRefsAttsImpl(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        if (recordIds.isEmpty()) {
            return emptyList()
        }
        val context: RequestContext = RequestContext.getCurrentNotNull()
        if (attributes.isEmpty()) {
            return recordIds.map { id: String -> RecordAtts(EntityRef.create(sourceId, id)) }
        }
        return getAttsFromSource(sourceId, recordIds, attributes, rawAtts, context)
    }

    private fun getAttsFromSource(
        sourceId: String,
        recs: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        context: RequestContext
    ): List<RecordAtts> {

        val recordsDao = getRecordsDaoPair(sourceId, RecordsAttsDao::class.java)
        if (recordsDao != null) {
            return getAttsFromRecordsDao(sourceId, recordsDao, recs, attributes, rawAtts, context)
        } else {
            val refs: List<EntityRef> = recs.map { EntityRef.create(sourceId, it) }
            return context.doWithVarNotNull(AttSchemaResolver.CTX_SOURCE_ID_KEY, sourceId) {
                recordsAttsService.getAtts(
                    recs.map { EmptyAttValue.INSTANCE },
                    attributes,
                    rawAtts,
                    EmptyMixinContext,
                    refs
                )
            }
        }
    }

    private fun getAttsFromRecordsDao(
        sourceId: String,
        recordsDao: Pair<RecordsDao, RecordsAttsDao>,
        recs: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        context: RequestContext
    ): List<RecordAtts> {

        var recAtts = recordsDao.second.getRecordsAtts(recs)
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
            return recs.map { RecordAtts(EntityRef.create(sourceId, it)) }
        } else {
            val mixins = if (recordsDao.first is AttMixinsHolder) {
                (recordsDao.first as AttMixinsHolder).getMixinContext()
            } else {
                EmptyMixinContext
            }
            val refs: List<EntityRef> = recs.map { EntityRef.create(sourceId, it) }

            val attsResult = context.doWithVarNotNull(AttSchemaResolver.CTX_SOURCE_ID_KEY, sourceId) {
                recordsAttsService.getAtts(recAtts, attributes, rawAtts, mixins, refs)
            }
            return sortGetAttsResultIfNeeded(sourceId, attsResult, recs)
        }
    }

    private fun sortGetAttsResultIfNeeded(
        sourceId: String,
        attributes: List<RecordAtts>,
        recordIds: List<String>
    ): List<RecordAtts> {
        if (attributes.size <= 1 || attributes.size != recordIds.size) {
            return attributes
        }
        var isSortingRequired = false
        for (idx in recordIds.indices) {
            val valueRef = attributes[idx].getId()
            val valueAppName = valueRef.getAppName()
            if (valueAppName.isNotBlank() && valueAppName != currentApp) {
                break
            }
            val valueSourceId = valueRef.getSourceId()
            if (valueSourceId.isNotBlank() && valueSourceId != sourceId) {
                break
            }
            val valueLocalId = valueRef.getLocalId()
            if (valueLocalId != recordIds[idx]) {
                // we found that id from atts doesn't match id from request
                // let's check that out atts id exists in request ids list
                // if it doesn't exist then sorting can't be performed
                for (innerIdx in (idx + 1) until recordIds.size) {
                    if (valueLocalId == recordIds[innerIdx]) {
                        isSortingRequired = true
                        break
                    }
                }
                break
            }
        }
        if (!isSortingRequired) {
            return attributes
        }
        val idxById = recordIds.mapIndexed { idx, id -> id to idx }.toMap()
        val valuesToSort = ArrayList<Pair<Int, RecordAtts>>(recordIds.size)
        for (atts in attributes) {
            val idx = idxById[atts.getId().getLocalId()] ?: return attributes
            valuesToSort.add(idx to atts)
        }
        valuesToSort.sortBy { it.first }
        return valuesToSort.map { it.second }
    }

    override fun mutateRecord(
        sourceId: String,
        record: LocalRecordAtts,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecordAtts {

        val selfAttValue = readSelfAttribute(record.getAtt(RecordConstants.ATT_SELF))
        val recordToMutate = if (!selfAttValue.isObject()) {
            record
        } else {
            val newAtts = ObjectData.create()
            record.attributes.forEach { key, value ->
                if (key == RecordConstants.ATT_SELF) {
                    selfAttValue.forEach { selfKey, selfValue ->
                        newAtts[selfKey] = selfValue
                    }
                } else {
                    newAtts[key] = value
                }
            }
            LocalRecordAtts(record.id, newAtts)
        }
        return if (interceptors.isEmpty()) {
            mutateRecordImpl(sourceId, recordToMutate, attsToLoad, rawAtts)
        } else {
            MutateRecordInterceptorsChain(this, interceptors.iterator())
                .invoke(sourceId, recordToMutate, attsToLoad, rawAtts)
        }
    }

    private fun readSelfAttribute(value: DataValue): DataValue {
        return if (value.isNull()) {
            value
        } else if (value.isArray()) {
            if (value.size() > 0) {
                readSelfAttribute(value[0])
            } else {
                DataValue.NULL
            }
        } else if (value.isObject()) {
            val url = value["url"]
            if (url.isTextual() && url.asText().startsWith(DataUriUtil.DATA_PREFIX)) {
                Json.mapper.read(url.asText(), ObjectData::class.java)?.getData() ?: DataValue.NULL
            } else {
                value
            }
        } else {
            DataValue.NULL
        }
    }

    internal fun mutateRecordImpl(
        sourceId: String,
        record: LocalRecordAtts,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean
    ): RecordAtts {

        val dao = needRecordsDaoPair(sourceId, RecordMutateWithAnyResDao::class.java)

        var mutAnyRes = dao.second.mutateForAnyRes(record)
            ?: return RecordAtts(EntityRef.create(sourceId, record.id))

        if (mutAnyRes is String && mutAnyRes.isNotEmpty()) {
            mutAnyRes = EntityRef.valueOf(mutAnyRes).withDefaultSourceId(sourceId)
        }

        var mutRes = if (attsToLoad.isEmpty()) {
            val defaultRef = EntityRef.create(sourceId, record.id)
            RecordAtts(recordsAttsService.getId(mutAnyRes, defaultRef))
        } else {

            recordsAttsService.doWithSchema(attsToLoad, rawAtts) { schema ->
                getValuesAtts(listOf(mutAnyRes), schema, rawAtts).first()
            }
        }
        val ref = mutRes.getId()
        if (StringUtils.isBlank(ref.getSourceId())) {
            mutRes = RecordAtts(mutRes, EntityRef.create(sourceId, ref.getLocalId()))
        }
        return mutRes
    }

    override fun deleteRecords(sourceId: String, recordIds: List<String>): List<DelStatus> {
        if (recordIds.isEmpty()) {
            return emptyList()
        }
        return if (interceptors.isEmpty()) {
            deleteRecordsImpl(sourceId, recordIds)
        } else {
            DeleteRecordsInterceptorsChain(this, interceptors.iterator()).invoke(sourceId, recordIds)
        }
    }

    internal fun deleteRecordsImpl(sourceId: String, recordIds: List<String>): List<DelStatus> {
        return needRecordsDaoPair(sourceId, RecordsDeleteDao::class.java).second.delete(recordIds)
    }

    override fun register(sourceId: String, recordsDao: RecordsDao) {

        allDao[sourceId] = recordsDao
        register(sourceId, attsDao, RecordsAttsDao::class.java, recordsDao)
        register(sourceId, queryDao, RecordsQueryResDao::class.java, recordsDao)
        register(sourceId, mutateDao, RecordMutateWithAnyResDao::class.java, recordsDao)
        register(sourceId, deleteDao, RecordsDeleteDao::class.java, recordsDao)

        if (sourceId == "") {
            hasDaoWithEmptyId = true
        }

        if (recordsDao is ServiceFactoryAware) {
            (recordsDao as ServiceFactoryAware).setRecordsServiceFactory(services)
        }
        if (recordsDao is JobsProvider) {
            val jobs: List<Job> = (recordsDao as JobsProvider).jobs
            for ((idx, job) in jobs.withIndex()) {
                jobExecutor.addJob(idx, sourceId, job)
            }
        }
    }

    override fun unregister(sourceId: String) {

        allDao.remove(sourceId)
        attsDao.remove(sourceId)
        queryDao.remove(sourceId)
        mutateDao.remove(sourceId)
        deleteDao.remove(sourceId)

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

        val recordsDao = allDao[sourceId] ?: return null

        val recordsSourceInfo = RecordsSourceMeta()
        recordsSourceInfo.id = sourceId

        if (recordsDao is SupportsQueryLanguages) {
            recordsSourceInfo.supportedLanguages = recordsDao.getSupportedLanguages()
        }
        recordsSourceInfo.features.query = queryDao[sourceId] != null
        recordsSourceInfo.features.mutate = mutateDao[sourceId] != null
        recordsSourceInfo.features.delete = deleteDao[sourceId] != null
        recordsSourceInfo.features.getAtts = attsDao[sourceId] != null

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
        return result
    }

    private fun <T : RecordsDao> getRecordsDaoPair(sourceId: String, type: Class<T>): Pair<RecordsDao, T>? {
        val mapByType = daoMapByType[type]
        var result = mapByType?.get(sourceId)
        if (result == null && sourceId.indexOf('/') == -1) {
            result = mapByType?.get("$currentApp/$sourceId")
        }
        @Suppress("UNCHECKED_CAST")
        return result as? Pair<RecordsDao, T>?
    }

    private fun <T : RecordsDao> needRecordsDaoPair(sourceId: String, type: Class<T>): Pair<RecordsDao, T> {
        return getRecordsDaoPair(sourceId, type) ?: throw RecordsSourceNotFoundException(sourceId, type)
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
        return hasDaoWithEmptyId
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

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        this.recordsTemplateService = serviceFactory.recordsTemplateService
    }
}
