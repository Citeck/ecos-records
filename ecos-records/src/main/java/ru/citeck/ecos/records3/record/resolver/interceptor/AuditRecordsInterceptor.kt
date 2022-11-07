package ru.citeck.ecos.records3.record.resolver.interceptor

import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

open class AuditRecordsInterceptor(services: RecordsServiceFactory) : LocalRecordsInterceptor {

    override fun query(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryInterceptorsChain
    ): RecsQueryRes<RecordAtts> {
        TODO("Not yet implemented")
    }

    override fun getValueAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetValueAttsInterceptorsChain
    ): List<RecordAtts> {
        TODO("Not yet implemented")
    }

    override fun getRecordAtts(
        sourceId: String,
        recordsId: List<String>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetRecordAttsInterceptorsChain
    ): List<RecordAtts> {
        TODO("Not yet implemented")
    }

    override fun mutateRecords(
        sourceId: String,
        records: List<LocalRecordAtts>,
        attsToLoad: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: MutateRecordsInterceptorsChain
    ): List<RecordAtts> {
        TODO("Not yet implemented")
    }

    override fun deleteRecords(
        sourceId: String,
        recordsId: List<String>,
        chain: DeleteInterceptorsChain
    ): List<DelStatus> {
        TODO("Not yet implemented")
    }

    /*    companion object {
        private const val APP_NAME = "appName"
        private const val APP_INSTANCE_ID = "appInstanceId"
        private const val SOURCE_ID = "sourceId"
        private const val LOCAL_ID = "localId"

        private val SYSTEM_SOURCES = setOf(
            RecordsSourceRecordsDao.ID,
            RecordsApiRecordsDao.ID
        )
    }

    private val attsWriter: AttSchemaWriter = services.attSchemaWriter
    private val currentAppName = services.webappProps.appName
    private val currentAppInstanceId = services.webappProps.appInstanceId

    private lateinit var beforeQueryEmitter: AuditEventEmitter<BeforeQueryEvent>
    private lateinit var afterQueryEmitter: AuditEventEmitter<QueryEvent>
    private lateinit var beforeGetAttsEmitter: AuditEventEmitter<GetAttsEvent>
    private lateinit var beforeMutateEmitter: AuditEventEmitter<MutateRecordEvent>
    private lateinit var beforeDeleteEmitter: AuditEventEmitter<DeleteRecordEvent>

    private var interceptorValid = false

    init {
        val auditApi = services.getEcosWebAppContext()?.getAuditApi()
        if (auditApi != null) {
            beforeQueryEmitter = auditApi.createEmitter(BeforeQueryEvent::class.java).build()
            afterQueryEmitter = auditApi.createEmitter(QueryEvent::class.java).build()
            beforeGetAttsEmitter = auditApi.createEmitter(GetAttsEvent::class.java).build()
            beforeMutateEmitter = auditApi.createEmitter(MutateRecordEvent::class.java).build()
            beforeDeleteEmitter = auditApi.createEmitter(DeleteRecordEvent::class.java).build()
            interceptorValid = true
        }
    }

    open fun isValid(): Boolean {
        return interceptorValid
    }

    override fun query(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryInterceptorsChain
    ): RecsQueryRes<RecordAtts> {
        beforeQueryEmitter.emit({ headers ->
            if (isEventShouldBeSkipped(queryArg.sourceId, attributes)) {
                null
            } else {
                headers[SOURCE_ID] = queryArg.sourceId
                headers[APP_NAME] = currentAppName
                headers[APP_INSTANCE_ID] = currentAppInstanceId
            }
        }) {
            BeforeQueryEvent(
                getGlobalSourceId(queryArg.sourceId),
                queryArg,
                writeSchemaForEvent(attributes)
            )
        }
        val queryStartMs = System.currentTimeMillis()
        val result = chain.invoke(queryArg, attributes, rawAtts)
        val queryDurationMs = System.currentTimeMillis() - queryStartMs
        afterQueryEmitter.emit({ headers ->
            if (isEventShouldBeSkipped(queryArg.sourceId, attributes)) {
                null
            } else {
                headers[SOURCE_ID] = queryArg.sourceId.substringAfter(EntityRef.APP_NAME_DELIMITER)
                headers[APP_NAME] = currentAppName
                headers[APP_INSTANCE_ID] = currentAppInstanceId
            }
        }) {
            QueryEvent(
                getGlobalSourceId(queryArg.sourceId),
                queryArg,
                result.getRecords().map { it.getId().withDefaultAppName(currentAppName) },
                writeSchemaForEvent(attributes),
                queryDurationMs
            )
        }
        return result
    }

    override fun getAtts(
        records: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetRecordAttsInterceptorsChain
    ): List<RecordAtts> {
        beforeGetAttsEmitter.emitForEach(records, {
            AttsCtx(writeSchemaForEvent(attributes))
        }, { _, record, headers ->
            if (record is EntityRef) {
                if (isEventShouldBeSkipped(record.getSourceId(), attributes)) {
                    null
                } else {
                    headers[SOURCE_ID] = record.getSourceId()
                    headers[LOCAL_ID] = record.getLocalId()
                    headers[APP_NAME] = currentAppName
                    headers[APP_INSTANCE_ID] = currentAppInstanceId
                    record
                }
            } else {
                null
            }
        }) { ctx, record ->
            GetAttsEvent(
                getGlobalSourceId(record.getSourceId()),
                record.withDefaultAppName(currentAppName),
                ctx.attributes
            )
        }
        return chain.invoke(records, attributes, rawAtts)
    }

    override fun mutate(
        records: List<RecordAtts>,
        attsToLoad: List<List<SchemaAtt>>,
        rawAtts: Boolean,
        chain: MutateRecordsInterceptorsChain
    ): List<RecordAtts> {
        beforeMutateEmitter.emitForEach(records.indices, {}, { _, idx, headers ->
            val record = records[idx]
            headers[SOURCE_ID] = record.getId().getSourceId()
            headers[LOCAL_ID] = record.getId().getLocalId()
            headers[APP_NAME] = currentAppName
            headers[APP_INSTANCE_ID] = currentAppInstanceId
            idx
        }) { _, idx ->
            val record = records[idx].withoutSensitiveData()
            MutateRecordEvent(
                getGlobalSourceId(record.getId()),
                record.getId().withDefaultAppName(currentAppName),
                record.getAtts(),
                writeSchemaForEvent(attsToLoad.getOrNull(idx))
            )
        }
        return chain.invoke(records, attsToLoad, rawAtts)
    }

    override fun delete(
        records: List<EntityRef>,
        chain: DeleteInterceptorsChain
    ): List<DelStatus> {
        beforeDeleteEmitter.emitForEach(records, {}, { _, record, headers ->
            headers[SOURCE_ID] = record.getSourceId()
            headers[LOCAL_ID] = record.getLocalId()
            headers[APP_NAME] = currentAppName
            headers[APP_INSTANCE_ID] = currentAppInstanceId
            record
        }) { _, record ->
            DeleteRecordEvent(
                getGlobalSourceId(record),
                record.withDefaultAppName(currentAppName)
            )
        }
        return chain.invoke(records)
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

    @AuditEventType("records.query")
    class QueryEvent(
        val sourceId: String,
        val query: RecordsQuery,
        val records: List<EntityRef>,
        val attributes: Map<String, String>
    )

    @AuditEventType("records.get-atts")
    class GetAttsEvent(
        val sourceId: String,
        val records: List<EntityRef>,
        val attributes: Map<String, String>
    )

    @AuditEventType("records.mutate")
    class MutateRecordEvent(
        val sourceId: String,
        val records: List<EntityRef>,
        val attributes: ObjectData,
        val attsToLoad: Map<String, String>
    )

    @AuditEventType("records.delete")
    class DeleteRecordEvent(
        val sourceId: String,
        val records: List<EntityRef>
    )*/
}
