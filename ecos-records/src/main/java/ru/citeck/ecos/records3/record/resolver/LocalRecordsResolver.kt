package ru.citeck.ecos.records3.record.resolver

import ecos.com.fasterxml.jackson210.databind.JsonNode
import lombok.Data
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.RecordsServiceFactory
import ru.citeck.ecos.records2.meta.AttributesSchema
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.querylang.QueryWithLang
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery
import ru.citeck.ecos.records2.source.dao.local.job.Job
import ru.citeck.ecos.records2.source.dao.local.job.JobExecutor
import ru.citeck.ecos.records2.utils.RecordsUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records2.request.query.RecordsQuery as RecordsQueryV0
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.impl.group.RecordsGroupDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixinsHolder
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttSchemaResolver
import ru.citeck.ecos.records3.record.op.delete.dao.RecordsDeleteDao
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordsMutateDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.utils.V1ConvUtils
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors

class LocalRecordsResolver(private val services: RecordsServiceFactory) {

    companion object {

        private val log = KotlinLogging.logger {}

        private val DEBUG_QUERY_TIME: String = "queryTimeMs"
        private val DEBUG_META_SCHEMA: String = "schema"
    }

    private val allDao = ConcurrentHashMap<String, RecordsDao>()
    private val attsDao = ConcurrentHashMap<String, RecordsAttsDao>()
    private val queryDao = ConcurrentHashMap<String, RecordsQueryDao>()
    private val mutateDao = ConcurrentHashMap<String, RecordsMutateDao>()
    private val deleteDao = ConcurrentHashMap<String, RecordsDeleteDao>()

    private val daoMapByType = mapOf<Class<*>, Map<String, RecordsDao>>(
        Pair(RecordsAttsDao::class.java, attsDao),
        Pair(RecordsQueryDao::class.java, queryDao),
        Pair(RecordsMutateDao::class.java, mutateDao),
        Pair(RecordsDeleteDao::class.java, deleteDao)
    )

    private val attProcService = services.attProcService
    private val attSchemaWriter = services.attSchemaWriter
    private val queryLangService = services.queryLangService
    private val recordsMetaService = services.recordsMetaService
    private val recordsAttsService = services.recordsAttsService
    private val localRecordsResolverV0 = services.localRecordsResolverV0

    private val converter: OneRecDaoConverter = OneRecDaoConverter()

    private val currentApp = services.properties.appName
    private val jobExecutor = JobExecutor(services)
    private val jobsInitialized = AtomicBoolean()

    fun initJobs(executor: ScheduledExecutorService) {
        if (jobsInitialized.compareAndSet(false, true)) {
            for (job in localRecordsResolverV0.getJobs()) {
                jobExecutor.addJob(job)
            }
            jobExecutor.init(executor)
        }
    }

    fun query(query: RecordsQuery, attributes: List<SchemaAtt>, rawAtts: Boolean): RecsQueryRes<RecordAtts> {

        var query = query
        val context: RequestContext = RequestContext.getCurrentNotNull()
        var sourceId = query.sourceId
        val appDelimIdx = sourceId.indexOf('/')

        if (appDelimIdx != -1) {
            val appName = sourceId.substring(0, appDelimIdx)
            if (appName == currentApp) {
                sourceId = sourceId.substring(appDelimIdx + 1)
                query = query.copy().withSourceId(sourceId).build()
            }
        }

        if (!allDao.containsKey(query.sourceId)) {

            val v0Query = V1ConvUtils.recsQueryV1ToV0(query, context)
            val attributesMap: Map<String, String> = attSchemaWriter.writeToMap(attributes)
            val schema: AttributesSchema = recordsMetaService.createSchema(attributesMap)
            val records = localRecordsResolverV0.queryRecords(v0Query, schema.getSchema())

            records.setRecords(recordsMetaService.convertMetaResult(records.getRecords(), schema, !rawAtts))
            records.setRecords(unescapeKeys(records.getRecords()))

            V1ConvUtils.addErrorMessages(records.getErrors(), context)
            V1ConvUtils.addDebugMessage(records, context)

            val queryRes = RecsQueryRes<RecordAtts>()
            queryRes.setRecords(
                Json.mapper.convert(records.getRecords(), Json.mapper.getListType(RecordAtts::class.java))
            )
            queryRes.hasMore = records.getHasMore()
            queryRes.totalCount = records.getTotalCount()

            return queryRes
        }

        val finalQuery = query
        var recordsResult: RecsQueryRes<RecordAtts>? = null

        if (query.groupBy.isNotEmpty()) {

            val dao = getRecordsDao(sourceId, RecordsQueryDao::class.java)

            if (dao == null || !dao.isGroupingSupported()) {

                val groupsSource = needRecordsDao(RecordsGroupDao.ID, RecordsQueryDao::class.java)
                val convertedQuery = updateQueryLanguage(query, groupsSource)

                if (convertedQuery == null) {

                    val errorMsg = ("GroupBy is not supported by language: "
                        + query.language + ". Query: " + query)
                    context.addMsg(MsgLevel.ERROR) { errorMsg }

                } else {

                    val queryRes: RecsQueryRes<*>? = groupsSource.queryRecords(convertedQuery)
                    if (queryRes != null) {

                        val atts: List<RecordAtts> = recordsAttsService.getAtts(
                            queryRes.getRecords(),
                            attributes,
                            rawAtts, emptyList())

                        recordsResult = RecsQueryRes(atts)
                        recordsResult.hasMore = queryRes.hasMore
                        recordsResult.totalCount = queryRes.totalCount
                    }
                }
            }
        } else {

            if (DistinctQuery.LANGUAGE == query.language) {

                val recordsQueryDao = getRecordsDao(sourceId, RecordsQueryDao::class.java)
                val languages = recordsQueryDao?.getSupportedLanguages() ?: emptyList()
                if (!languages.contains(DistinctQuery.LANGUAGE)) {
                    val distinctQuery: DistinctQuery = query.getQuery(DistinctQuery::class.java)
                    recordsResult = RecsQueryRes()
                    recordsResult.setRecords(getDistinctValues(sourceId,
                        distinctQuery,
                        finalQuery.page.maxItems,
                        attributes
                    ))
                }
            } else {
                val dao = getRecordsDao(sourceId, RecordsQueryDao::class.java)
                if (dao == null) {
                    val msg = "RecordsQueryDao is not found for id = '$sourceId'"
                    context.addMsg(MsgLevel.ERROR) { msg }
                } else {
                    recordsResult = RecsQueryRes()
                    query = updateQueryLanguage(query, dao)!!
                    val queryRes: RecsQueryRes<*>? = dao.queryRecords(query)
                    if (queryRes != null) {
                        val objMixins = if (dao is AttMixinsHolder) {
                            (dao as AttMixinsHolder).getMixins()
                        } else {
                            emptyList()
                        }
                        val recAtts: List<RecordAtts> = context.doWithVar(
                            AttSchemaResolver.CTX_SOURCE_ID_KEY,
                            query.sourceId
                        ) {
                            getAtts(queryRes.getRecords(), attributes, rawAtts, objMixins)
                        }
                        recordsResult.setRecords(recAtts)
                        recordsResult.totalCount = queryRes.totalCount
                        recordsResult.hasMore = queryRes.hasMore
                    }
                }
            }
        }
        if (recordsResult == null) {
            recordsResult = RecsQueryRes()
        }
        return RecordsUtils.attsWithDefaultApp(recordsResult, currentApp)
    }

    private fun getDistinctValues(sourceId: String,
                                  distinctQuery: DistinctQuery,
                                  max: Int,
                                  schema: List<SchemaAtt>): List<RecordAtts> {
        var max = max
        if (max == -1) {
            max = 50
        }
        var recordsQuery = RecordsQuery.create()
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .withSourceId(sourceId)
            .withMaxItems(Math.max(max, 20))
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

        val distinctAttSchema: List<SchemaAtt> = listOf(SchemaAtt.create()
            .withAlias("att")
            .withName(distinctQuery.attribute)
            .withInner(innerSchema)
            .build())
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
                        distinctPredicate!!.addPredicate(Predicates.eq(distinctAtt, attStr))
                    }
                }
            }
        } while (found > 0 && values.size <= max && ++requests <= max)

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

    private fun updateQueryLanguage(recordsQuery: RecordsQuery, dao: RecordsQueryDao?): RecordsQuery? {
        var recordsQuery = recordsQuery
        if (dao == null) {
            return null
        }
        val supportedLanguages = dao.getSupportedLanguages()
        if (supportedLanguages.isEmpty()) {
            return recordsQuery
        }
        val queryWithLangOpt: Optional<QueryWithLang?> = queryLangService.convertLang(recordsQuery.query,
            recordsQuery.language,
            supportedLanguages)
        if (queryWithLangOpt.isPresent) {
            val queryWithLang: QueryWithLang = queryWithLangOpt.get()
            return recordsQuery.copy {
                query = DataValue.create(queryWithLang.query)
                language = queryWithLang.language
            }
        }
        return null
    }

    fun getAtts(records: List<*>,
                attributes: List<SchemaAtt>,
                rawAtts: Boolean): List<RecordAtts> {

        return getAtts(records, attributes, rawAtts, emptyList())
    }

    private fun getAtts(records: List<*>,
                        attributes: List<SchemaAtt>,
                        rawAtts: Boolean,
                        mixinsForObjects: List<AttMixin>): List<RecordAtts> {

        val context = RequestContext.getCurrentNotNull()

        if (log.isDebugEnabled) {
            log.debug("getMeta start.\nRecords: $records attributes: $attributes")
        }
        val recordObjs = ArrayList<ValWithIdx<Any>>()
        val recordRefs = ArrayList<ValWithIdx<RecordRef>>()
        var idx = 0
        for (rec in records) {
            if (rec is RecordRef) {
                var ref = rec
                if (ref.appName == currentApp) {
                    ref = ref.removeAppName()
                }
                recordRefs.add(ValWithIdx<RecordRef>(ref, idx))
            } else {
                recordObjs.add(ValWithIdx<Any>(rec, idx))
            }
            idx++
        }
        val result: List<ValWithIdx<RecordAtts>> = ArrayList<ValWithIdx<RecordAtts>>()
        val refsAtts: List<RecordAtts>? = getMetaImpl(recordRefs.map { obj -> obj.value }, attributes, rawAtts)

        if (refsAtts != null && refsAtts.size == recordRefs.size) {
            for (i in refsAtts.indices) {
                result.add(ValWithIdx(refsAtts[i], recordRefs[i].getIdx()))
            }
        } else {
            context.addMsg(MsgLevel.ERROR) {
                "Results count doesn't match with " +
                    "requested. refsAtts: " + refsAtts + " recordRefs: " + recordRefs
            }
        }
        val atts: List<RecordAtts> = recordsAttsService.getAtts(recordObjs.stream()
            .map(Function<ValWithIdx<Any?>?, Any?> { obj: ValWithIdx<Any?>? -> obj.getValue() })
            .collect(Collectors.toList()), attributes, rawAtts, mixinsForObjects)
        if (atts != null && atts.size == recordObjs.size) {
            for (i in atts.indices) {
                result.add(ValWithIdx<RecordAtts>(atts[i], recordObjs[i].getIdx()))
            }
        } else {
            context.addMsg(MsgLevel.ERROR) {
                "Results count doesn't match with " +
                    "requested. atts: " + atts + " recordObjs: " + recordObjs
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("getMeta end.\nRecords: $records attributes: $attributes")
        }
        result.sort(Comparator.comparingInt(ToIntFunction<ValWithIdx<RecordAtts>?> { obj: ValWithIdx<RecordAtts>? -> obj.getIdx() }))
        return result.stream()
            .map(Function<ValWithIdx<RecordAtts>?, RecordAtts> { obj: ValWithIdx<RecordAtts>? -> obj.getValue() })
            .collect(Collectors.toList())
    }

    private fun getMetaImpl(records: Collection<RecordRef>,
                            attributes: List<SchemaAtt>,
                            rawAtts: Boolean): List<RecordAtts> {

        if (records.isEmpty()) {
            return emptyList()
        }
        val context: RequestContext = RequestContext.getCurrentNotNull()
        if (attributes.isEmpty()) {
            return records.map { id: RecordRef -> RecordAtts(id) }
        }
        val results = ArrayList<ValWithIdx<RecordAtts>>()

        RecordsUtils.groupRefBySource(records).forEach( { sourceId: String, recs: List<ValWithIdx<RecordRef>> ->
            val recordsDao: RecordsAttsDao = getRecordsDao<RecordsAttsDao?>(sourceId, RecordsAttsDao::class.java).orElse(null)
            if (recordsDao == null) {
                val sourceIdRefs: List<RecordRef?> = recs!!.stream()
                    .map(Function<ValWithIdx<RecordRef?>?, RecordRef?> { obj: ValWithIdx<RecordRef?>? -> obj.getValue() })
                    .collect(Collectors.toList())
                val processors: Map<String, List<AttProcDef?>?> = HashMap<String, List<AttProcDef?>?>()
                val attsWithoutProcessors: List<SchemaAtt?> = ArrayList<SchemaAtt?>()
                attributes.forEach(Consumer<SchemaAtt?> { att: SchemaAtt? ->
                    processors[att.getAliasForValue()] = att.processors
                    attsWithoutProcessors.add(AttUtils.removeProcessors(att))
                })
                val attributesMap: Map<String, String> = attSchemaWriter.writeToMap(attsWithoutProcessors)
                val schema: AttributesSchema = recordsMetaService.createSchema(attributesMap)
                var attsList: List<RecordAtts>? = null
                try {
                    val meta: RecordsResult<RecordMeta> = localRecordsResolverV0.getMeta(sourceIdRefs, schema.getSchema())
                    meta.setRecords(recordsMetaService.convertMetaResult(meta.getRecords(), schema, !rawAtts))
                    meta.setRecords(unescapeKeys(meta.getRecords()))
                    V1ConvUtils.addErrorMessages(meta.getErrors(), context)
                    V1ConvUtils.addDebugMessage(meta, context)
                    attsList = Json.mapper.convert(meta.getRecords(), Json.mapper!!.getListType(RecordAtts::class.java))
                    if (attsList != null && !processors.isEmpty()) {
                        attsList = attsList.stream()
                            .map(Function<RecordAtts, RecordMeta> { r: RecordAtts ->
                                RecordMeta(r.getId(),
                                    attProcService.applyProcessors(r.getAtts(), processors))
                            }
                            ).collect(Collectors.toList())
                    }
                } catch (e: Throwable) {
                    log.error("Local records resolver v0 error. " +
                        "SourceId: '" + sourceId + "' recs: " + recs, e)
                    context.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
                }
                if (attsList == null) {
                    attsList = emptyList()
                }
                if (attsList!!.size != sourceIdRefs.size) {
                    context.addMsg(MsgLevel.ERROR) { "getMetaImpl request failed. SourceId: '$sourceId' Records: $sourceIdRefs" }
                    for (ref in recs) {
                        results.add(ref.map(Function<RecordRef?, RecordAtts> { id: RecordRef? -> RecordAtts(id) }))
                    }
                } else {
                    for (i in attsList.indices) {
                        results.add(ValWithIdx<RecordAtts>(attsList[i], recs[i].getIdx()))
                    }
                }
            } else {
                var recAtts = recordsDao.getRecordsAtts(recs!!.stream()
                    .map(Function<ValWithIdx<RecordRef?>?, String> { r: ValWithIdx<RecordRef?>? -> r.getValue().id })
                    .collect(Collectors.toList())) as List<Any?>
                if (recAtts == null) {
                    recAtts = ArrayList()
                    for (ignored in recs) {
                        recAtts.add(EmptyAttValue.Companion.INSTANCE)
                    }
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
                        results.add(ref.map(Function<RecordRef?, RecordAtts> { id: RecordRef? -> RecordAtts(id) }))
                    }
                } else {
                    var mixins: List<AttMixin?> = emptyList()
                    if (recordsDao is AttMixinsHolder) {
                        mixins = (recordsDao as AttMixinsHolder).getMixins()
                    }
                    val refs: List<RecordRef?> = recs.stream().map(Function<ValWithIdx<RecordRef?>?, RecordRef?> { obj: ValWithIdx<RecordRef?>? -> obj.getValue() }).collect(Collectors.toList())
                    val atts: List<RecordAtts> = recordsAttsService.getAtts(recAtts, attributes, rawAtts, mixins, refs)
                    for (i in recs.indices) {
                        results.add(ValWithIdx<RecordAtts>(atts[i], i))
                    }
                }
            }
        })
        results.sort(Comparator.comparingInt(ToIntFunction<ValWithIdx<RecordAtts>?> { obj: ValWithIdx<RecordAtts>? -> obj.getIdx() }))
        return results.stream().map(Function<ValWithIdx<RecordAtts>?, RecordAtts> { obj: ValWithIdx<RecordAtts>? -> obj.getValue() }).collect(Collectors.toList())
    }

    private fun unescapeKeys(meta: List<RecordMeta>): List<RecordMeta> {
        return meta!!.stream().map(Function<RecordMeta, RecordMeta> { r: RecordMeta ->
            var jsonNode: JsonNode = r.getAtts().getData().asJson()
            jsonNode = attSchemaWriter.unescapeKeys(jsonNode)
            RecordMeta(r.getId(), ObjectData.create(jsonNode))
        }).collect(Collectors.toList())
    }

    fun mutate(records: List<RecordAtts>): List<RecordRef?> {
        val daoResult: List<RecordRef?> = ArrayList<RecordRef?>()
        val refsMapping: Map<RecordRef?, RecordRef?> = HashMap<RecordRef?, RecordRef?>()
        records.forEach(Consumer<RecordAtts> { record: RecordAtts ->
            val appName: String = record.getId().sourceId
            val sourceId: String = record.getId().sourceId
            if (currentApp == appName) {
                val newId: RecordRef = record.getId().removeAppName()
                refsMapping[newId] = record.getId()
                record = RecordAtts(record, newId)
            }
            val dao: RecordsMutateDao = getRecordsDao<RecordsMutateDao?>(sourceId, RecordsMutateDao::class.java).orElse(null)
            if (dao == null) {
                val mutation = RecordsMutation()
                mutation.setRecords(listOf(RecordMeta(record)))
                val mutateRes: RecordsMutResult = localRecordsResolverV0.mutate(mutation)
                if (mutateRes.getRecords() == null || mutateRes.getRecords().isEmpty()) {
                    daoResult.add(record.getId())
                } else {
                    daoResult.add(mutateRes.getRecords().get(0).getId())
                }
            } else {
                val localRef: RecordRef = RecordRef.create("", record.getId().id)
                var mutRes: List<RecordRef?>? = dao.mutate(listOf(RecordAtts(record, localRef)))
                if (mutRes == null) {
                    mutRes = listOf(record.getId())
                } else {
                    mutRes = mutRes.stream()
                        .map(Function<RecordRef?, RecordRef?> { r: RecordRef? ->
                            if (isBlank(r.sourceId)) {
                                return@map RecordRef.create(sourceId, r.id)
                            }
                            r
                        }).collect(Collectors.toList())
                }
                daoResult.addAll(mutRes!!)
            }
        })
        var result: List<RecordRef?> = daoResult
        if (!refsMapping.isEmpty()) {
            result = daoResult
                .stream()
                .map(Function<RecordRef?, RecordRef?> { ref: RecordRef? -> refsMapping.getOrDefault(ref, ref) })
                .collect(Collectors.toList())
        }
        return result
    }

    fun delete(records: List<RecordRef?>): List<DelStatus?> {
        val daoResult: List<DelStatus?> = ArrayList<DelStatus?>()
        records.forEach(Consumer<RecordRef?> { record: RecordRef? ->
            val sourceId: String = record.sourceId
            val dao: RecordsDeleteDao = needRecordsDao<RecordsDeleteDao?>(sourceId, RecordsDeleteDao::class.java)
            val delResult: List<DelStatus?> = dao.delete(listOf<String>(record.id))
            daoResult.add(if (delResult != null && !delResult.isEmpty()) delResult[0] else DelStatus.OK)
        })
        return daoResult
    }

    private fun getDaoWithQuery(query: RecordsQuery?): ru.citeck.ecos.records3.record.resolver.LocalRecordsResolver.DaoWithConvQuery? {
        var sourceId = query!!.sourceId
        val sourceDelimIdx: Int = sourceId.indexOf(RecordRef.SOURCE_DELIMITER)
        var innerSourceId = ""
        if (sourceDelimIdx > 0) {
            innerSourceId = sourceId!!.substring(sourceDelimIdx + 1)
            sourceId = sourceId.substring(0, sourceDelimIdx)
        }
        val dao = needRecordsDao<RecordsQueryDao?>(sourceId, RecordsQueryDao::class.java)
        var convertedQuery = updateQueryLanguage(query, dao)
            ?: throw LanguageNotSupportedException(sourceId, query.language)
        convertedQuery = RecordsQuery(convertedQuery)
        convertedQuery.sourceId = innerSourceId
        return ru.citeck.ecos.records3.record.resolver.LocalRecordsResolver.DaoWithConvQuery(dao, convertedQuery)
    }

    fun register(sourceId: String, recordsDao: RecordsDao?) {
        if (sourceId == null) {
            log.error("id is a mandatory parameter for RecordsDao."
                + " Type: " + recordsDao!!.javaClass
                + " toString: " + recordsDao)
            return
        }
        allDao!![sourceId] = recordsDao
        register<RecordsAttsDao?>(sourceId, attsDao, RecordsAttsDao::class.java, recordsDao)
        register<RecordsQueryDao?>(sourceId, queryDao, RecordsQueryDao::class.java, recordsDao)
        register<RecordsMutateDao?>(sourceId, mutateDao, RecordsMutateDao::class.java, recordsDao)
        register<RecordsDeleteDao?>(sourceId, deleteDao, RecordsDeleteDao::class.java, recordsDao)
        if (recordsDao is ServiceFactoryAware) {
            (recordsDao as ServiceFactoryAware?).setRecordsServiceFactory(serviceFactory)
        }
        if (recordsDao is JobsProvider) {
            val jobs: List<Job?> = (recordsDao as JobsProvider?).getJobs()
            for (job in jobs) {
                jobExecutor.addJob(job)
            }
        }
    }

    private fun <T : RecordsDao?> register(id: String,
                                           registry: Map<String, T?>?,
                                           type: Class<T?>?,
                                           value: RecordsDao?) {
        var value = value
        value = converter!!.convertOneToMultiDao(value)
        if (type!!.isAssignableFrom(value!!.javaClass)) {
            val dao = value as T?
            registry!![id] = dao
        }
    }

    fun getSourceInfo(sourceId: String): RecordsDaoInfo? {
        val recordsDao = allDao!![sourceId] ?: return localRecordsResolverV0.getSourceInfo(sourceId)
        val recordsSourceInfo = RecordsDaoInfo()
        recordsSourceInfo.id = sourceId
        if (recordsDao is RecordsQueryDao) {
            recordsSourceInfo.supportedLanguages = recordsDao.supportedLanguages
            recordsSourceInfo.features.query = true
        } else {
            recordsSourceInfo.features.query = false
        }
        recordsSourceInfo.features.mutate = recordsDao is RecordsMutateDao
        recordsSourceInfo.features.delete = recordsDao is RecordsDeleteDao
        recordsSourceInfo.features.getAtts = recordsDao is RecordsAttsDao
        val columnsSourceId: ColumnsSourceId = recordsDao.javaClass.getAnnotation(ColumnsSourceId::class.java)
        if (columnsSourceId != null && isNotBlank(columnsSourceId.value())) {
            recordsSourceInfo.columnsSourceId = columnsSourceId.value()
        }
        return recordsSourceInfo
    }

    val sourceInfo: List<Any?>
        get() {
            val result: List<RecordsDaoInfo?> = ArrayList<RecordsDaoInfo?>()
            allDao!!.keys
                .stream()
                .map(Function<String, RecordsDaoInfo?> { sourceId: String -> getSourceInfo(sourceId!!) })
                .forEach(Consumer<RecordsDaoInfo?> { e: RecordsDaoInfo? -> result.add(e) })
            result.addAll(localRecordsResolverV0.getSourceInfo())
            return result
        }

    private fun <T : RecordsDao> getRecordsDao(sourceId: String, type: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return daoMapByType[type] as? T
    }

    private fun <T : RecordsDao> needRecordsDao(sourceId: String, type: Class<T>): T {
        return getRecordsDao(sourceId, type) ?: error("Records source is not found: $sourceId")
    }

    fun containsDao(id: String): Boolean {
        return allDao.containsKey(id)
    }

    private class QueryResult {
        private val result: RecsQueryRes<*> = null
        private val recordsDao: RecordsDao? = null
    }

    private class DaoWithConvQuery(val dao: RecordsQueryDao?, val query: RecordsQuery?)

}
