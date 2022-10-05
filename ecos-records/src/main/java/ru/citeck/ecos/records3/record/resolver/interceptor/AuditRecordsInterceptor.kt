package ru.citeck.ecos.records3.record.resolver.interceptor

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
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
import java.util.LinkedHashMap

open class AuditRecordsInterceptor(services: RecordsServiceFactory) : LocalRecordsInterceptor {

    companion object {
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
    private lateinit var afterQueryEmitter: AuditEventEmitter<AfterQueryEvent>
    private lateinit var beforeGetAttsEmitter: AuditEventEmitter<BeforeGetAttsEvent>
    private lateinit var beforeMutateEmitter: AuditEventEmitter<BeforeMutateRecordEvent>
    private lateinit var beforeDeleteEmitter: AuditEventEmitter<BeforeDeleteRecordEvent>

    private var interceptorValid = false

    init {
        val auditApi = services.getEcosWebAppContext()?.getAuditApi()
        if (auditApi != null) {
            beforeQueryEmitter = auditApi.createEmitter(BeforeQueryEvent::class.java).build()
            afterQueryEmitter = auditApi.createEmitter(AfterQueryEvent::class.java).build()
            beforeGetAttsEmitter = auditApi.createEmitter(BeforeGetAttsEvent::class.java).build()
            beforeMutateEmitter = auditApi.createEmitter(BeforeMutateRecordEvent::class.java).build()
            beforeDeleteEmitter = auditApi.createEmitter(BeforeDeleteRecordEvent::class.java).build()
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
            AfterQueryEvent(
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
        chain: GetAttsInterceptorsChain
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
            BeforeGetAttsEvent(
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
        chain: MutateInterceptorsChain
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
            BeforeMutateRecordEvent(
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
            BeforeDeleteRecordEvent(
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

    @AuditEventType("records.query.before")
    class BeforeQueryEvent(
        val sourceId: String,
        val query: RecordsQuery,
        val attributes: Map<String, String>
    )

    @AuditEventType("records.query.after")
    class AfterQueryEvent(
        val sourceId: String,
        val query: RecordsQuery,
        val records: List<EntityRef>,
        val attributes: Map<String, String>,
        val queryDurationMs: Long
    )

    @AuditEventType("records.get-atts.before")
    class BeforeGetAttsEvent(
        val sourceId: String,
        val record: EntityRef,
        val attributes: Map<String, String>
    )

    @AuditEventType("records.mutate.before")
    class BeforeMutateRecordEvent(
        val sourceId: String,
        val record: EntityRef,
        val attributes: ObjectData,
        val attsToLoad: Map<String, String>
    )

    @AuditEventType("records.delete.before")
    class BeforeDeleteRecordEvent(
        val sourceId: String,
        val record: EntityRef
    )

    private class AttsCtx(
        val attributes: Map<String, String>
    )
}
