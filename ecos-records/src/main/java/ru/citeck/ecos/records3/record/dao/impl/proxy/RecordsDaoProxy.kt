package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttSchemaUtils
import ru.citeck.ecos.records3.record.atts.value.AttValue
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
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.dao.txn.TxnRecordsDao
import ru.citeck.ecos.records3.record.resolver.LocalRemoteResolver
import java.util.*
import kotlin.collections.LinkedHashMap

open class RecordsDaoProxy(
    private val id: String,
    private val targetId: String,
    private val processor: ProxyProcessor? = null
) : AbstractRecordsDao(),
    RecordsQueryResDao,
    RecordsAttsDao,
    RecordsMutateDao,
    RecordsDeleteDao,
    HasClientMeta,
    TxnRecordsDao {

    private val attsProc = processor as? AttsProxyProcessor
    private val mutProc = processor as? MutateProxyProcessor
    private val clientMetaProc = processor as? HasClientMeta
    private lateinit var recordsResolver: LocalRemoteResolver

    override fun getRecordsAtts(recordsId: List<String>): List<*>? {

        val contextAtts = getContextAtts()
        val attsFromTarget = recordsService.getAtts(toTargetRefs(recordsId), contextAtts, true)

        return postProcessAtts(attsFromTarget)
    }

    private fun postProcessAtts(attsFromTarget: List<RecordAtts>): List<AttValue> {

        val proxyTargetAtts = attsFromTarget.map { ProxyRecordAtts(it) }
        val postProcAtts = attsProc?.postProcessAtts(proxyTargetAtts) ?: proxyTargetAtts

        if (postProcAtts.size != attsFromTarget.size) {
            error(
                "Post process additional attributes should has " +
                    "the same size with records from argument. Id: $id Records: ${attsFromTarget.map { it.getId() }}"
            )
        }

        return postProcAtts.map { proxyAtts ->
            val innerAttValue = InnerAttValue(proxyAtts.atts.getAtts().getData().asJson())
            val ref = RecordRef.create(id, proxyAtts.atts.getId().id)
            ProxyRecVal(ref, innerAttValue, proxyAtts.additionalAtts)
        }
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {

        val contextAtts = getContextAtts()

        val targetQuery = recsQuery.copy().withSourceId(targetId).build()

        return if (contextAtts.isEmpty()) {
            val result = recordsService.query(targetQuery)
            result.setRecords(
                result.getRecords().map {
                    RecordRef.create(id, it.id)
                }
            )
            result
        } else {
            val queryRes = recordsService.query(targetQuery, contextAtts, true)
            val queryResWithAtts = RecsQueryRes<Any>()
            queryResWithAtts.setHasMore(queryRes.getHasMore())
            queryResWithAtts.setTotalCount(queryRes.getTotalCount())
            queryResWithAtts.setRecords(postProcessAtts(queryRes.getRecords()))
            queryResWithAtts
        }
    }

    override fun delete(recordsId: List<String>): List<DelStatus> {
        return recordsService.delete(toTargetRefs(recordsId))
    }

    override fun mutate(records: List<LocalRecordAtts>): List<String> {

        val recsToMutate = mutProc?.prepareMutation(records) ?: records

        return recordsService.mutate(
            recsToMutate.map {
                RecordAtts(toTargetRef(it.id), it.attributes)
            }
        ).map {
            it.id
        }
    }

    private fun toTargetRefs(recordsId: List<String>): List<RecordRef> {
        return recordsId.map { toTargetRef(it) }
    }

    private fun toTargetRef(recordId: String): RecordRef {
        return RecordRef.valueOf("$targetId@$recordId")
    }

    private fun getContextAtts(): Map<String, String> {

        var schemaAtts = AttSchemaUtils.simplifySchema(AttContext.getCurrentSchemaAtt().inner)
        schemaAtts = attsProc?.prepareAttsSchema(schemaAtts) ?: schemaAtts

        val writer = serviceFactory.attSchemaWriter
        val result = LinkedHashMap<String, String>()

        schemaAtts.forEach { att ->
            result[att.getAliasForValue()] = writer.write(att)
        }

        return result
    }

    override fun getId(): String {
        return id
    }

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        super.setRecordsServiceFactory(serviceFactory)
        if (processor is ServiceFactoryAware) {
            processor.setRecordsServiceFactory(serviceFactory)
        }
        recordsResolver = serviceFactory.recordsResolver
    }

    override fun getClientMeta(): ClientMeta? {
        return clientMetaProc?.getClientMeta()
    }

    override fun commit(txnId: UUID, recordsId: List<String>) {
        recordsResolver.commit(recordsId.map { toTargetRef(it) })
    }

    override fun rollback(txnId: UUID, recordsId: List<String>) {
        recordsResolver.rollback(recordsId.map { toTargetRef(it) })
    }

    override fun isTransactional(): Boolean {
        return recordsResolver.isSourceTransactional(targetId)
    }

    private class ProxyRecVal(
        private val id: RecordRef,
        base: AttValue,
        val postProcAtts: Map<String, Any>
    ) : AttValueDelegate(base) {

        override fun getId(): Any {
            return id
        }

        override fun getAtt(name: String?): Any? {
            if (postProcAtts.containsKey(name)) {
                return postProcAtts[name]
            }
            return super.getAtt(name)
        }
    }
}
