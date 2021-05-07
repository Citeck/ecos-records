package ru.citeck.ecos.records3.record.dao.impl.proxy

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.impl.AttValueDelegate
import ru.citeck.ecos.records3.record.atts.value.impl.InnerAttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryResDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

open class RecordsDaoProxy(
    private val id: String,
    private val targetId: String,
    processor: ProxyProcessor? = null
) : AbstractRecordsDao(),
    RecordsQueryResDao,
    RecordsAttsDao,
    RecordsMutateDao,
    RecordsDeleteDao {

    private val attsProc = processor as? AttsProxyProcessor
    private val mutProc = processor as? MutateProxyProcessor

    override fun getRecordsAtts(recordsId: List<String>): List<*>? {

        val contextAtts = getContextAtts()
        val attsFromTarget = recordsService.getAtts(toTargetRefs(recordsId), contextAtts, true)

        return postProcessAtts(attsFromTarget)
    }

    private fun postProcessAtts(attsFromTarget: List<RecordAtts>): List<AttValue> {

        val postProcAtts = attsProc?.postProcessAtts(attsFromTarget)
        if (!postProcAtts.isNullOrEmpty() && postProcAtts.size != attsFromTarget.size) {
            error(
                "Post process additional attributes should has " +
                    "the same size with records from argument. Id: $id Records: ${attsFromTarget.map { it.getId() }}"
            )
        }

        return if (postProcAtts.isNullOrEmpty()) {
            attsFromTarget.map { InnerAttValue(it.getAtts().getData().asJson()) }
        } else {
            var idx = 0
            attsFromTarget.map {
                val innerAtts = InnerAttValue(it.getAtts().getData().asJson())
                val additionalAtts = postProcAtts[idx++]
                if (additionalAtts.isEmpty()) {
                    innerAtts
                } else {
                    ProxyRecVal(innerAtts, additionalAtts)
                }
            }
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

        val recsToMutate = if (mutProc != null) {
            val recsCopy = records.map { LocalRecordAtts(it.id, it.attributes.deepCopy()) }
            mutProc.prepareMutation(recsCopy)
            recsCopy
        } else {
            records
        }

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
        val contextAtts = AttContext.getInnerAttsMap()
        return attsProc?.let {
            val atts = LinkedHashMap(contextAtts)
            it.prepareAtts(atts)
            atts
        } ?: contextAtts
    }

    override fun getId(): String {
        return id
    }

    private class ProxyRecVal(
        base: AttValue,
        val postProcAtts: Map<String, Any>
    ) : AttValueDelegate(base) {

        override fun getAtt(name: String?): Any? {
            if (postProcAtts.containsKey(name)) {
                return postProcAtts[name]
            }
            return super.getAtt(name)
        }
    }
}
