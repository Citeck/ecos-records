package ru.citeck.ecos.records3.record.resolver.interceptor

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.api.RecordsApiRecordsDao
import ru.citeck.ecos.records3.record.dao.impl.source.RecordsSourceRecordsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.audit.AuditEventEmitter
import ru.citeck.ecos.webapp.api.audit.AuditEventType
import ru.citeck.ecos.webapp.api.entity.EntityRef

open class AuditRecordsInterceptor(services: RecordsServiceFactory) : LocalRecordsInterceptor {

    companion object {

        private const val APP_NAME = "appName"
        private const val APP_INSTANCE_ID = "appInstanceId"
        private const val SOURCE_ID = "sourceId"

        private val SYSTEM_SOURCES = setOf(
            RecordsSourceRecordsDao.ID,
            RecordsApiRecordsDao.ID
        )
    }

    private val attsWriter: AttSchemaWriter = services.attSchemaWriter
    private val currentAppName = services.webappProps.appName
    private val currentAppInstanceId = services.webappProps.appInstanceId

    private lateinit var queryRecordsEmitter: AuditEventEmitter<QueryRecordsEvent>
    private lateinit var getRecordAttsEmitter: AuditEventEmitter<GetRecordsAttsEvent>
    private lateinit var mutateRecordEmitter: AuditEventEmitter<MutateRecordEvent>
    private lateinit var deleteRecordsEmitter: AuditEventEmitter<DeleteRecordsEvent>

    private var interceptorValid = false

    init {
        val auditApi = services.getEcosWebAppApi()?.getAuditApi()
        if (auditApi != null) {
            queryRecordsEmitter = auditApi.createEmitter(QueryRecordsEvent::class.java).build()
            getRecordAttsEmitter = auditApi.createEmitter(GetRecordsAttsEvent::class.java).build()
            mutateRecordEmitter = auditApi.createEmitter(MutateRecordEvent::class.java).build()
            deleteRecordsEmitter = auditApi.createEmitter(DeleteRecordsEvent::class.java).build()
            interceptorValid = true
        }
    }

    open fun isValid(): Boolean {
        return interceptorValid
    }

    override fun queryRecords(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryRecordsInterceptorsChain
    ): RecsQueryRes<RecordAtts> {
        if (!queryRecordsEmitter.isEnabled() || isEventShouldBeSkipped(queryArg.sourceId, attributes)) {
            return chain.invoke(queryArg, attributes, rawAtts)
        }
        val context = queryRecordsEmitter.createContext {
            chain.invoke(queryArg, attributes, rawAtts)
        }
        val headers = context.getHeaders()
        headers[SOURCE_ID] = queryArg.sourceId.substringAfter(EntityRef.APP_NAME_DELIMITER)
        headers[APP_NAME] = currentAppName
        headers[APP_INSTANCE_ID] = currentAppInstanceId

        val actionResult = context.getActionResult()
        if (context.isEventRequired()) {
            val records = if (!actionResult.isCompletedExceptionally()) {
                actionResult.getResult().getRecords().map {
                    var id = it.getId()
                    if (id.sourceId.isEmpty()) {
                        id = id.withSourceId(queryArg.sourceId)
                    }
                    id.withDefaultAppName(currentAppName)
                }
            } else {
                emptyList()
            }
            context.sendEvent(
                QueryRecordsEvent(
                    getGlobalSourceId(queryArg.sourceId),
                    queryArg,
                    records,
                    writeSchemaForEvent(attributes)
                )
            )
        }
        return actionResult.getResult()
    }

    override fun getRecordsAtts(
        sourceId: String,
        recordIds: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetRecordsAttsInterceptorsChain
    ): List<RecordAtts> {
        if (!getRecordAttsEmitter.isEnabled() || isEventShouldBeSkipped(sourceId, attributes)) {
            return chain.invoke(sourceId, recordIds, attributes, rawAtts)
        }
        val context = getRecordAttsEmitter.createContext {
            chain.invoke(sourceId, recordIds, attributes, rawAtts)
        }
        val headers = context.getHeaders()
        headers[SOURCE_ID] = getGlobalSourceId(sourceId)
        headers[APP_NAME] = currentAppName
        headers[APP_INSTANCE_ID] = currentAppInstanceId
        if (context.isEventRequired()) {
            context.sendEvent(
                GetRecordsAttsEvent(
                    getGlobalSourceId(sourceId),
                    recordIds.map {
                        EntityRef.create(sourceId, it).withDefaultAppName(currentAppName)
                    },
                    writeSchemaForEvent(attributes)
                )
            )
        }
        return context.getActionResult().getResult()
    }

    override fun getValuesAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetValuesAttsInterceptorsChain
    ): List<RecordAtts> {
        return chain.invoke(values, attributes, rawAtts)
    }

    override fun mutateRecord(
        sourceId: String,
        record: LocalRecordAtts,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: MutateRecordInterceptorsChain
    ): RecordAtts {
        if (!mutateRecordEmitter.isEnabled()) {
            return chain.invoke(sourceId, record, attsToLoad, rawAtts)
        }
        val context = mutateRecordEmitter.createContext {
            chain.invoke(sourceId, record, attsToLoad, rawAtts)
        }
        val globalSrcId = getGlobalSourceId(sourceId)
        val headers = context.getHeaders()
        headers[SOURCE_ID] = globalSrcId
        headers[APP_NAME] = currentAppName
        headers[APP_INSTANCE_ID] = currentAppInstanceId
        if (context.isEventRequired()) {
            val refToMutate = EntityRef.create(sourceId, record.id).withDefaultAppName(currentAppName)
            val resultRef = context.getActionResult().getResult().getId().withDefaultAppName(currentAppName)
            val attributes = record.withoutSensitiveData().attributes
            context.sendEvent(
                MutateRecordEvent(
                    globalSrcId,
                    resultRef,
                    refToMutate,
                    resultRef.getLocalId().isNotEmpty() && refToMutate != resultRef,
                    attributes,
                    writeSchemaForEvent(attsToLoad)
                )
            )
        }
        return context.getActionResult().getResult()
    }

    override fun deleteRecords(
        sourceId: String,
        recordIds: List<String>,
        chain: DeleteRecordsInterceptorsChain
    ): List<DelStatus> {

        if (!deleteRecordsEmitter.isEnabled()) {
            return chain.invoke(sourceId, recordIds)
        }

        val context = deleteRecordsEmitter.createContext {
            chain.invoke(sourceId, recordIds)
        }

        val globalSrcId = getGlobalSourceId(sourceId)

        val headers = context.getHeaders()
        headers[SOURCE_ID] = globalSrcId
        headers[APP_NAME] = currentAppName
        headers[APP_INSTANCE_ID] = currentAppInstanceId

        if (context.isEventRequired()) {
            context.sendEvent(
                DeleteRecordsEvent(
                    globalSrcId,
                    recordIds.map { EntityRef.create(sourceId, it).withDefaultAppName(currentAppName) }
                )
            )
        }
        return context.getActionResult().getResult()
    }

    private fun getGlobalSourceId(sourceId: String): String {
        return if (sourceId.contains(EntityRef.APP_NAME_DELIMITER)) {
            sourceId
        } else {
            currentAppName + EntityRef.APP_NAME_DELIMITER + sourceId
        }
    }

    private fun getGlobalSourceId(ref: EntityRef): String {
        val appName = ref.getAppName().ifBlank { currentAppName }
        return appName + EntityRef.APP_NAME_DELIMITER + ref.getSourceId()
    }

    private fun isEventShouldBeSkipped(sourceId: String, attsToLoad: List<SchemaAtt>): Boolean {
        if (!SYSTEM_SOURCES.contains(sourceId)) {
            return false
        }
        return attsToLoad.all { !it.name.startsWith("$") }
    }

    private fun writeSchemaForEvent(schema: List<SchemaAtt>?): Map<String, String> {
        if (schema.isNullOrEmpty()) {
            return emptyMap()
        }
        val result: MutableMap<String, String> = LinkedHashMap()
        schema.forEach { att: SchemaAtt -> result[att.name] = attsWriter.write(att) }
        return result
    }

    @AuditEventType("records.query-records")
    class QueryRecordsEvent(
        val sourceId: String,
        val query: RecordsQuery,
        val records: List<EntityRef>,
        val attributes: Map<String, String>
    )

    @AuditEventType("records.get-records-atts")
    class GetRecordsAttsEvent(
        val sourceId: String,
        val records: List<EntityRef>,
        val attributes: Map<String, String>
    )

    @AuditEventType("records.mutate-record")
    class MutateRecordEvent(
        val sourceId: String,
        val record: EntityRef,
        val recToMutate: EntityRef,
        val newRecord: Boolean,
        val attributes: ObjectData,
        val attsToLoad: Map<String, String>
    )

    @AuditEventType("records.delete-records")
    class DeleteRecordsEvent(
        val sourceId: String,
        val records: List<EntityRef>
    )
}
