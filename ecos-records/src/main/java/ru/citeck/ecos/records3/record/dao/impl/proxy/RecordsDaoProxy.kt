package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValueProxy
import ru.citeck.ecos.records3.record.atts.value.impl.AttValueDelegate
import ru.citeck.ecos.records3.record.atts.value.impl.InnerAttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.records3.record.dao.impl.source.client.ClientMeta
import ru.citeck.ecos.records3.record.dao.impl.source.client.HasClientMeta
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryResDao
import ru.citeck.ecos.records3.record.dao.query.RecsGroupQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.dao.txn.TxnRecordsDao
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.resolver.LocalRemoteResolver
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*
import kotlin.collections.LinkedHashMap

open class RecordsDaoProxy(
    id: String,
    targetId: String,
    processor: ProxyProcessor? = null
) : AbstractRecordsDao(addGlobalMixins = false),
    RecordsQueryResDao,
    RecordsAttsDao,
    RecordsMutateDao,
    RecordsDeleteDao,
    HasClientMeta,
    TxnRecordsDao,
    RecsGroupQueryDao {

    // fields in constructor cause exception
    // Incorrect resolution sequence for Java field public open val id: kotlin.String defined in
    // ru.citeck.ecos.SomeClass[JavaForKotlinOverridePropertyDescriptor@3fbf66e7] (source = null)
    private val idField: String = id
    private val targetIdField: String = targetId
    private val processorField: ProxyProcessor? = processor

    private val attsProc = processor as? AttsProxyProcessor
    private val mutProc = processor as? MutateProxyProcessor
    private val delProc = processor as? DeleteProxyProcessor
    private val clientMetaProc = processor as? HasClientMeta
    private lateinit var recordsResolver: LocalRemoteResolver

    protected var sourceIdMapping: Map<String, String> = emptyMap()

    override fun getRecordsAtts(recordIds: List<String>): List<*>? {

        val procContext = ProxyProcContext()
        val contextAtts = getContextAtts(procContext)
        val attsFromTarget = doWithSourceIdMapping {
            recordsService.getAtts(toTargetRefs(recordIds), contextAtts, true)
        }

        return postProcessAtts(attsFromTarget, procContext)
    }

    protected open fun postProcessAtts(attsFromTarget: List<RecordAtts>, procContext: ProxyProcContext): List<AttValue> {

        val proxyTargetAtts = attsFromTarget.map { ProxyRecordAtts(it) }
        val postProcAtts = attsProc?.attsPostProcess(proxyTargetAtts, procContext) ?: proxyTargetAtts

        if (postProcAtts.size != attsFromTarget.size) {
            error(
                "Post process additional attributes should has " +
                    "the same size with records from argument. Id: $idField Records: ${attsFromTarget.map { it.getId() }}"
            )
        }

        return postProcAtts.map { proxyAtts ->
            val innerAttValue = InnerAttValue(proxyAtts.atts.getAtts().getData().asJson())
            val ref = RecordRef.create(idField, proxyAtts.atts.getId().id)
            ProxyRecVal(ref, innerAttValue, proxyAtts.additionalAtts)
        }
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {

        val procContext = ProxyProcContext()
        val contextAtts = getContextAtts(procContext)

        val targetQuery = recsQuery.copy().withSourceId(targetIdField).build()

        return if (contextAtts.isEmpty()) {
            val result = doWithSourceIdMapping {
                recordsService.query(targetQuery)
            }
            result.setRecords(
                result.getRecords().map {
                    RecordRef.create(idField, it.id)
                }
            )
            result
        } else {
            val queryRes = doWithSourceIdMapping {
                recordsService.query(targetQuery, contextAtts, true)
            }
            val queryResWithAtts = RecsQueryRes<Any>()
            queryResWithAtts.setHasMore(queryRes.getHasMore())
            queryResWithAtts.setTotalCount(queryRes.getTotalCount())
            queryResWithAtts.setRecords(postProcessAtts(queryRes.getRecords(), procContext))
            queryResWithAtts
        }
    }

    protected open fun <T> doWithSourceIdMapping(action: () -> T): T {
        return RequestContext.doWithCtx({
            it.withSourceIdMapping(sourceIdMapping)
        }) {
            action.invoke()
        }
    }

    override fun delete(recordIds: List<String>): List<DelStatus> {

        val procContext = ProxyProcContext()
        delProc?.deletePreProcess(recordIds, procContext)

        val statuses = doWithSourceIdMapping {
            recordsService.delete(toTargetRefs(recordIds))
        }

        delProc?.deletePostProcess(recordIds, statuses, procContext)

        return statuses
    }

    override fun mutate(records: List<LocalRecordAtts>): List<String> {

        val procContext = ProxyProcContext()
        val recsToMutate = mutProc?.mutatePreProcess(records, procContext) ?: records

        val resultRefs = mutateWithoutProcessing(recsToMutate)

        val processedRefs = mutProc?.mutatePostProcess(resultRefs, procContext) ?: resultRefs
        if (processedRefs.size != resultRefs.size) {
            error("RecordRefs size was changed by processor: ${mutProc?.javaClass}")
        }
        return processedRefs.map { it.id }
    }

    open fun mutateWithoutProcessing(records: List<LocalRecordAtts>): List<RecordRef> {
        return doWithSourceIdMapping {
            recordsService.mutate(
                records.map {
                    RecordAtts(toTargetRef(it.id), it.attributes)
                }
            )
        }
    }

    protected open fun toTargetRefs(recordIds: List<String>): List<RecordRef> {
        return recordIds.map { toTargetRef(it) }
    }

    protected open fun toTargetRef(recordId: String): RecordRef {
        return RecordRef.valueOf("$targetIdField@$recordId")
    }

    protected open fun getContextAtts(procContext: ProxyProcContext): Map<String, String> {

        var schemaAtts = AttContext.getCurrentSchemaAtt().inner
        schemaAtts = attsProc?.attsPreProcess(schemaAtts, procContext) ?: schemaAtts

        val writer = serviceFactory.attSchemaWriter
        val result = LinkedHashMap<String, String>()

        schemaAtts.forEach { att ->
            result[att.getAliasForValue()] = writer.write(att)
        }

        return result
    }

    fun getProcessor(): ProxyProcessor? {
        return processorField
    }

    override fun getId(): String {
        return idField
    }

    fun getTargetId(): String {
        return targetIdField
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        super.setRecordsServiceFactory(serviceFactory)
        if (processorField is ServiceFactoryAware) {
            processorField.setRecordsServiceFactory(serviceFactory)
        }
        recordsResolver = serviceFactory.recordsResolver
        processorField?.init(this)

        val currentAppName = serviceFactory.webappProps.appName
        fun addDefaultAppName(srcId: String): String {
            if (currentAppName.isBlank()) {
                return srcId
            }
            val appNameDelimIdx = srcId.indexOf(EntityRef.APP_NAME_DELIMITER)
            if (appNameDelimIdx != -1) {
                return srcId
            }
            return currentAppName + EntityRef.APP_NAME_DELIMITER + srcId
        }
        sourceIdMapping = mapOf(addDefaultAppName(targetIdField) to addDefaultAppName(idField))
    }

    override fun getClientMeta(): ClientMeta? {
        return clientMetaProc?.getClientMeta()
    }

    override fun commit(txnId: UUID, recordIds: List<String>) {
        recordsResolver.commit(recordIds.map { toTargetRef(it) })
    }

    override fun rollback(txnId: UUID, recordIds: List<String>) {
        recordsResolver.rollback(recordIds.map { toTargetRef(it) })
    }

    override fun isTransactional(): Boolean {
        return recordsResolver.isSourceTransactional(targetIdField)
    }

    private class ProxyRecVal(
        private val id: RecordRef,
        base: AttValue,
        val postProcAtts: Map<String, Any?>
    ) : AttValueDelegate(base), AttValueProxy {

        override fun getId(): Any {
            return id
        }

        override fun getAtt(name: String): Any? {
            if (postProcAtts.containsKey(name)) {
                return postProcAtts[name]
            }
            return super.getAtt(name)
        }
    }
}
