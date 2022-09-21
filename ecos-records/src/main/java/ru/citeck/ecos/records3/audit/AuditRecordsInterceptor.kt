package ru.citeck.ecos.records3.audit

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.client.ClientContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.resolver.interceptor.*
import ru.citeck.ecos.webapp.api.audit.AuditEventType
import ru.citeck.ecos.webapp.api.audit.EcosAuditService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.LocalDateTime
import javax.annotation.PostConstruct

class AuditRecordsInterceptor(
    private val recordsServiceFactory: RecordsServiceFactory
) : LocalRecordsInterceptor {

    private val auditService: EcosAuditService by lazy {
        recordsServiceFactory.getEcosWebAppContext()?.getAuditService() ?: error("EcosWebAppContext is null")
    }

    private val queryEmitter by lazy { auditService.getEmitter<QueryRecordEvent>(QueryRecordEvent::class.java) }
    private val getAttsEmitter by lazy { auditService.getEmitter<GetAttsEvent>(GetAttsEvent::class.java) }
    private val mutateEmitter by lazy { auditService.getEmitter<MutateRecordEvent>(MutateRecordEvent::class.java) }
    private val deleteEmitter by lazy { auditService.getEmitter<DeleteRecordEvent>(DeleteRecordEvent::class.java) }

    @PostConstruct
    fun register() {
        recordsServiceFactory.localRecordsResolver.addInterceptor(this)
    }

    override fun query(
        queryArg: RecordsQuery,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: QueryInterceptorsChain
    ): RecsQueryRes<RecordAtts> {
        queryEmitter.emit {
            QueryRecordEvent(
                queryArg.sourceId,
                queryArg.query,
                attributes.map { it.name }
            )
        }
        return chain.invoke(queryArg, attributes, rawAtts)
    }

    override fun getAtts(
        records: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        chain: GetAttsInterceptorsChain
    ): List<RecordAtts> {
        getAttsEmitter.emit {
            GetAttsEvent(records, attributes.map { it.name })
        }
        return chain.invoke(records, attributes, rawAtts)
    }

    override fun mutate(
        records: List<RecordAtts>,
        attsToLoad: List<List<SchemaAtt>>,
        rawAtts: Boolean,
        chain: MutateInterceptorsChain
    ): List<RecordAtts> {
        mutateEmitter.emit {
            MutateRecordEvent(
                records.map {
                    it.withoutSensitiveData()
                }
            )
        }
        return chain.invoke(records, attsToLoad, rawAtts)
    }

    override fun delete(
        records: List<EntityRef>,
        chain: DeleteInterceptorsChain
    ): List<DelStatus> {
        deleteEmitter.emit {
            DeleteRecordEvent(records)
        }
        return chain.invoke(records)
    }

    @AuditEventType("records.query")
    class QueryRecordEvent(
        val sourceId: String,
        val query: DataValue,
        val attributes: List<String>
    ) : BaseAuditEventData()

    @AuditEventType("records.attributes")
    class GetAttsEvent(
        val records: List<*>,
        val attributes: List<String>
    ) : BaseAuditEventData()

    @AuditEventType("records.mutate")
    class MutateRecordEvent(
        val attributes: List<RecordAtts>,
    ) : BaseAuditEventData()

    @AuditEventType("records.delete")
    class DeleteRecordEvent(
        val records: List<EntityRef>
    ) : BaseAuditEventData()

    open class BaseAuditEventData {
        val dateTime: String = LocalDateTime.now().toString()
        val ipAddress: String = ClientContext.getClientData().ip
        val userId: String = AuthContext.getCurrentUser()
    }
}
