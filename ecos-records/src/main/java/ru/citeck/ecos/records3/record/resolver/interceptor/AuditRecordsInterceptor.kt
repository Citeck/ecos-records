package ru.citeck.ecos.records3.record.resolver.interceptor

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.audit.AuditEventEmitter
import ru.citeck.ecos.webapp.api.audit.AuditEventType
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.LinkedHashMap

open class AuditRecordsInterceptor(services: RecordsServiceFactory) : LocalRecordsInterceptor {

    companion object {
        private const val SOURCE_ID = "sourceId"
        private const val LOCAL_ID = "localId"
    }

    private val attsWriter: AttSchemaWriter = services.attSchemaWriter

    private lateinit var beforeQueryEmitter: AuditEventEmitter<BeforeQueryEvent>
    private lateinit var afterQueryEmitter: AuditEventEmitter<AfterQueryEvent>
    private lateinit var beforeGetAttsEmitter: AuditEventEmitter<BeforeGetAttsEvent>
    private lateinit var beforeMutateEmitter: AuditEventEmitter<BeforeMutateRecordEvent>
    private lateinit var beforeDeleteEmitter: AuditEventEmitter<BeforeDeleteRecordEvent>

    private var interceptorValid = false

    init {
        val auditService = services.getEcosWebAppContext()?.getAuditService()
        if (auditService != null) {
            beforeQueryEmitter = auditService.createEmitter(BeforeQueryEvent::class.java).build()
            afterQueryEmitter = auditService.createEmitter(AfterQueryEvent::class.java).build()
            beforeGetAttsEmitter = auditService.createEmitter(BeforeGetAttsEvent::class.java).build()
            beforeMutateEmitter = auditService.createEmitter(BeforeMutateRecordEvent::class.java).build()
            beforeDeleteEmitter = auditService.createEmitter(BeforeDeleteRecordEvent::class.java).build()
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
            headers[SOURCE_ID] = queryArg.sourceId
            true
        }) {
            BeforeQueryEvent(
                queryArg,
                writeSchemaForEvent(attributes)
            )
        }
        val queryStartMs = System.currentTimeMillis()
        val result = chain.invoke(queryArg, attributes, rawAtts)
        val queryDurationMs = System.currentTimeMillis() - queryStartMs
        afterQueryEmitter.emit({ headers ->
            headers[SOURCE_ID] = queryArg.sourceId
            true
        }) {
            AfterQueryEvent(
                queryArg,
                result.getRecords().map { it.getId() },
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
                headers[SOURCE_ID] = record.getSourceId()
                headers[LOCAL_ID] = record.getLocalId()
                record
            } else {
                null
            }
        }) { ctx, record ->
            BeforeGetAttsEvent(record, ctx.attributes)
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
            idx
        }) { _, idx ->
            val record = records[idx].withoutSensitiveData()
            BeforeMutateRecordEvent(
                record.getId(),
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
            record
        }) { _, record -> BeforeDeleteRecordEvent(record) }
        return chain.invoke(records)
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
        val query: RecordsQuery,
        val attributes: Map<String, String>
    )

    @AuditEventType("records.query.after")
    class AfterQueryEvent(
        val query: RecordsQuery,
        val records: List<EntityRef>,
        val attributes: Map<String, String>,
        val queryDurationMs: Long
    )

    @AuditEventType("records.get-atts.before")
    class BeforeGetAttsEvent(
        val record: EntityRef,
        val attributes: Map<String, String>
    )

    @AuditEventType("records.mutate.before")
    class BeforeMutateRecordEvent(
        val record: EntityRef,
        val attributes: ObjectData,
        val attsToLoad: Map<String, String>
    )

    @AuditEventType("records.delete.before")
    class BeforeDeleteRecordEvent(
        val record: EntityRef
    )

    private class AttsCtx(
        val attributes: Map<String, String>
    )
}
