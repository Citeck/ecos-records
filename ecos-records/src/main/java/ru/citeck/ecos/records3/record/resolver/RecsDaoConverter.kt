package ru.citeck.ecos.records3.record.resolver

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao
import ru.citeck.ecos.records3.record.op.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.op.delete.dao.RecordDeleteDao
import ru.citeck.ecos.records3.record.op.delete.dao.RecordsDeleteDao
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDao
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordsMutateCrossSrcDao
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordsMutateDao
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*

class RecsDaoConverter {

    companion object {
        val log = KotlinLogging.logger {}
    }

    fun convert(dao: RecordsDao): RecordsDao {

        if (dao is RecordAttsDao) {
            return mapToMultiDao(dao)
        }
        if (dao is RecordDeleteDao) {
            return mapToMultiDao(dao)
        }
        if (dao is RecordMutateDao) {
            return mapToMultiDao(dao)
        }
        if (dao is RecordsMutateDao) {
            return mapToMultiDao(dao)
        }
        if (dao is RecordMutateDtoDao<*>) {
            @Suppress("UNCHECKED_CAST")
            return mapToMutateCrossSrc(dao as RecordMutateDtoDao<Any>)
        }
        return dao
    }

    private fun mapToMultiDao(dao: RecordAttsDao): RecordsAttsDao {

        return object : RecordsAttsDao {

            override fun getRecordsAtts(records: List<String>): List<*> {
                return mapElements(
                    records,
                    { dao.getRecordAtts(it) },
                    { EmptyAttValue.INSTANCE },
                    { _, _ -> ObjectData.create() }
                )
            }
            override fun getId(): String = dao.getId()
        }
    }

    fun mapToMultiDao(dao: RecordDeleteDao): RecordsDeleteDao {

        return object : RecordsDeleteDao {

            override fun delete(records: List<String>): List<DelStatus> {
                return mapElements(
                    records,
                    { dao.delete(it) },
                    { DelStatus.OK },
                    { _, e -> throw e }
                )
            }

            override fun getId(): String = dao.getId()
        }
    }

    fun mapToMultiDao(dao: RecordMutateDao): RecordsMutateCrossSrcDao {

        return object : RecordsMutateCrossSrcDao {

            override fun mutate(records: List<LocalRecordAtts>): List<RecordRef> {
                return mapElements(
                    records,
                    { RecordRef.create(dao.getId(), dao.mutate(it)) },
                    { RecordRef.create(dao.getId(), it.id) },
                    { _, e -> throw e }
                )
            }

            override fun getId(): String = dao.getId()
        }
    }

    fun mapToMultiDao(dao: RecordsMutateDao): RecordsMutateCrossSrcDao {

        return object : RecordsMutateCrossSrcDao {

            override fun mutate(records: List<LocalRecordAtts>): List<RecordRef> {
                return dao.mutate(records).map { RecordRef.create(dao.getId(), it) }
            }

            override fun getId(): String = dao.getId()
        }
    }

    private fun <T : Any> mapToMutateCrossSrc(dao: RecordMutateDtoDao<T>): RecordsMutateCrossSrcDao {

        return object : RecordsMutateCrossSrcDao {

            override fun mutate(records: List<LocalRecordAtts>): List<RecordRef> {
                return records.map {
                    val record = dao.getRecToMutate(it.id)
                    Json.mapper.applyData(record, it.attributes)
                    val resultId = dao.saveMutatedRec(record)
                    RecordRef.create(dao.getId(), resultId)
                }
            }

            override fun getId(): String = dao.getId()
        }
    }

    private fun <T, R> mapElements(
        input: List<T>,
        mapFunc: (T) -> R,
        onEmpty: (T) -> R,
        onError: (T, Throwable) -> R
    ): List<R> {

        val result: MutableList<R> = ArrayList()
        for (value in input) {
            try {
                var res = mapFunc.invoke(value)
                if (res == null) {
                    res = onEmpty.invoke(value)
                }
                result.add(res)
            } catch (e: Throwable) {
                log.error("Mapping failed", e)
                RequestContext.getCurrentNotNull().addMsg(MsgLevel.ERROR) {
                    ErrorUtils.convertException(e)
                }
                result.add(onError.invoke(value, e))
            }
        }
        return result
    }
}