package ru.citeck.ecos.records3

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter
import ru.citeck.ecos.records3.record.dao.HasSourceIdAliases
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.record.resolver.LocalRemoteResolver
import ru.citeck.ecos.records3.record.type.RecordTypeService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis

open class RecordsServiceImpl(
    private val services: RecordsServiceFactory
) : AbstractRecordsService(), ServiceFactoryAware {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private lateinit var recordsResolver: LocalRemoteResolver
    private lateinit var dtoSchemaReader: DtoSchemaReader
    private lateinit var attSchemaWriter: AttSchemaWriter
    private lateinit var recordTypeService: RecordTypeService

    private val isGatewayMode = services.webappProps.gatewayMode
    private val currentAppName = services.webappProps.appName
    private val currentAppRef = currentAppName + ":" + services.webappProps.appInstanceId

    /* QUERY */

    override fun query(query: RecordsQuery): RecsQueryRes<EntityRef> {
        return handleRecordsQuery {
            val metaResult = recordsResolver.query(query, emptyMap<String, Any>(), true)
            metaResult.withRecords { it.getId() }
        }
    }

    override fun <T : Any> query(query: RecordsQuery, attributes: Class<T>): RecsQueryRes<T> {
        val schema = dtoSchemaReader.read(attributes)
        require(schema.isNotEmpty()) {
            "Attributes class doesn't have any fields with setter. Class: $attributes"
        }
        val queryRes: RecsQueryRes<RecordAtts> = query(query, attSchemaWriter.writeToMap(schema))
        return queryRes.withRecords { dtoSchemaReader.instantiateNotNull(attributes, it.getAtts()) }
    }

    override fun query(query: RecordsQuery, attributes: Map<String, *>, rawAtts: Boolean): RecsQueryRes<RecordAtts> {
        return handleRecordsQuery { recordsResolver.query(query, attributes, rawAtts) }
    }

    /* ATTRIBUTES */

    override fun getAtts(records: Collection<*>, attributes: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {
        return RequestContext.doWithCtx(services, { ctx -> ctx.withReadOnly(true) }) { ctx ->
            try {
                recordsResolver.getAtts(ArrayList(records), attributes, rawAtts)
            } catch (e: Throwable) {
                if (ctx.ctxData.omitErrors) {
                    ctx.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e, services) }

                    val emptyAtts = ObjectData.create()
                    attributes.keys.forEach { emptyAtts[it] = DataValue.NULL }

                    val result = ArrayList<RecordAtts>(records.size)
                    for (record in records) {
                        result.add(RecordAtts(tryToGetRecordRef(record), emptyAtts.deepCopy()))
                    }
                    result
                } else {
                    throw e
                }
            }
        }
    }

    private fun tryToGetRecordRef(record: Any?): EntityRef {
        record ?: return EntityRef.EMPTY
        if (record is EntityRef) {
            return record
        }
        return EntityRef.EMPTY
    }

    override fun <T : Any> getAtts(records: Collection<*>, attributes: Class<T>): List<T> {

        val schema = dtoSchemaReader.read(attributes)
        if (schema.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. AttsClass: $attributes")
        }
        val attsValues = getAtts(records, attSchemaWriter.writeToMap(schema))
        return attsValues.map {
            dtoSchemaReader.instantiate(attributes, it.getAtts())
                ?: error("Attributes class can't be instantiated. Class: $attributes Schema: $schema")
        }
    }

    /* MUTATE */

    override fun create(sourceIdOrType: String, attributes: Any): EntityRef {
        val sourceId = getSourceIdFromTypeOrSourceId(sourceIdOrType)
        return mutate(EntityRef.valueOf(sourceId + EntityRef.SOURCE_ID_DELIMITER), attributes)
    }

    private fun getSourceIdFromTypeOrSourceId(sourceIdOrType: String): String {
        if (sourceIdOrType.contains("type@")) {
            return recordTypeService.getSourceId(sourceIdOrType)
        }
        return sourceIdOrType
    }

    override fun <T : Any> mutateAndGetAtts(record: Any, attributes: Any, attsToLoad: Class<T>): T {
        val schema = dtoSchemaReader.read(attsToLoad)
        require(schema.isNotEmpty()) {
            "Attributes class doesn't have any fields with setter. Class: $attributes"
        }
        val meta = mutateAndGetAtts(record, attributes, attSchemaWriter.writeToMap(schema))
        return dtoSchemaReader.instantiate(attsToLoad, meta.getAtts())
            ?: error("Attributes class can't be instantiated. Class: $attsToLoad Schema: $schema")
    }

    override fun mutateAndGetAtts(
        records: List<RecordAtts>,
        attsToLoad: List<Map<String, *>>,
        rawAtts: Boolean
    ): List<RecordAtts> {

        val mutateResult: List<RecordAtts>
        val time = measureTimeMillis {
            mutateResult = RequestContext.doWithCtx(services) {
                if (records.isEmpty()) {
                    emptyList()
                } else {
                    val context = RequestContext.getCurrentNotNull()
                    if (context.ctxData.readOnly) {
                        error("Mutation is not allowed in read-only mode. Records: " + records.map { it.getId() })
                    }
                    val result = recordsResolver.mutateForAllApps(records.map { it.deepCopy() }, attsToLoad, rawAtts)

                    result.map { it.withDefaultAppName(currentAppName) }
                }
            }
        }

        log.trace {
            "Mutate records: ${records.map { it.withoutSensitiveData() }} in $time ms"
        }

        return mutateResult
    }

    /* DELETE */

    override fun delete(records: List<EntityRef>): List<DelStatus> {
        return RequestContext.doWithCtx(services) { deleteImpl(records) }
    }

    private fun deleteImpl(records: List<EntityRef>): List<DelStatus> {
        val context = RequestContext.getCurrentNotNull()
        if (context.ctxData.readOnly) {
            error("Deletion is not allowed in read-only mode. Records: $records")
        }
        val status = recordsResolver.delete(records)
        return status
    }

    /* OTHER */

    private fun <T : Any> handleRecordsQuery(supplier: () -> RecsQueryRes<T>): RecsQueryRes<T> {
        return RequestContext.doWithCtx(services, { ctx -> ctx.withReadOnly(true) }) { ctx ->
            try {
                supplier.invoke()
            } catch (e: Throwable) {
                if (ctx.ctxData.omitErrors) {
                    ctx.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e, services) }
                    RecsQueryRes()
                } else {
                    throw e
                }
            }
        }
    }

    override fun register(recordsSource: RecordsDao) {
        register(recordsSource.getId(), recordsSource)
        if (recordsSource is HasSourceIdAliases) {
            recordsSource.getSourceIdAliases().forEach {
                if (recordsResolver.getSourceInfo(it) == null) {
                    register(it, recordsSource)
                }
            }
        }
    }

    override fun register(sourceId: String, recordsSource: RecordsDao) {
        if (sourceId.isEmpty()) {
            log.error { "RecordsDAO with empty sourceId won't be registered: $recordsSource" }
        } else {
            recordsResolver.register(sourceId, recordsSource)
        }
    }

    override fun unregister(sourceId: String) {
        recordsResolver.unregister(sourceId)
    }

    override fun <T : Any> getRecordsDao(sourceId: String, type: Class<T>): T? {
        return recordsResolver.getRecordsDao(sourceId, type)
    }

    override fun getRecordsDao(sourceId: String): RecordsDao? {
        return getRecordsDao(sourceId, RecordsDao::class.java)
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {

        recordsResolver = serviceFactory.recordsResolver
        dtoSchemaReader = serviceFactory.dtoSchemaReader
        attSchemaWriter = serviceFactory.attSchemaWriter
        recordTypeService = serviceFactory.recordTypeService
        recordsResolver.setRecordsService(this)

        for (dao in serviceFactory.defaultRecordsDao) {
            if (dao is RecordsDao) {
                register(dao)
            }
        }
    }
}
