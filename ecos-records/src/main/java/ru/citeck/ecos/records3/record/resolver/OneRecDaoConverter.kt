package ru.citeck.ecos.records3.record.resolver

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.op.delete.dao.RecordDeleteDao
import ru.citeck.ecos.records3.record.op.delete.dao.RecordsDeleteDao
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDao
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordsMutateDao
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*

class OneRecDaoConverter {

    companion object {
        val log = KotlinLogging.logger {}
    }

    fun convertOneToMultiDao(dao: RecordsDao): RecordsDao {

        if (dao is RecordAttsDao) {
            return mapToMultiDao(dao)
        }
        if (dao is RecordDeleteDao) {
            return mapToMultiDao(dao)
        }
        return if (dao is RecordMutateDao) {
            mapToMultiDao(dao)
        } else {
            dao
        }
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

    fun mapToMultiDao(dao: RecordMutateDao): RecordsMutateDao {

        return object : RecordsMutateDao {

            override fun mutate(records: List<RecordAtts>): List<RecordRef> {
                return mapElements(
                    records,
                    { dao.mutate(it) },
                    { it.getId() },
                    { _, e -> throw e }
                )
            }

            override fun getId(): String = dao.getId()
        }
    }

    private fun <T, R> mapElements(input: List<T>,
                                   mapFunc: (T) -> R,
                                   onEmpty: (T) -> R,
                                   onError: (T, Throwable) -> R): List<R> {

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
