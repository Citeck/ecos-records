package ru.citeck.ecos.records3.record.resolver

import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.*
import java.util.function.Function
import java.util.function.Supplier


@Slf4j
class OneRecDaoConverter {
    fun convertOneToMultiDao(dao: RecordsDao?): RecordsDao? {
        if (dao is RecordAttsDao) {
            return mapToMultiDao(dao as RecordAttsDao?)
        }
        if (dao is RecordDeleteDao) {
            return mapToMultiDao(dao as RecordDeleteDao?)
        }
        return if (dao is RecordMutateDao) {
            mapToMultiDao(dao as RecordMutateDao?)
        } else dao
    }

    private fun mapToMultiDao(dao: RecordAttsDao?): RecordsAttsDao? {
        return object : RecordsAttsDao {
            override fun getRecordsAtts(records: MutableList<String?>): MutableList<*>? {
                return mapElements<String?, Any?>(
                    records, Function { record: String? -> dao.getRecordAtts(record) },
                    Function<String?, Any?> { v: String? -> EmptyAttValue.Companion.INSTANCE },
                    BiFunction<String?, Exception?, Any?> { v: String?, e: Exception? -> ObjectData.create() }
                )
            }

            val id: String?
                get() = dao.getId()
        }
    }

    fun mapToMultiDao(dao: RecordDeleteDao?): RecordsDeleteDao? {
        return object : RecordsDeleteDao {
            override fun delete(records: MutableList<String?>): MutableList<DelStatus?>? {
                return mapElements(
                    records, Function { recordId: String? -> dao.delete(recordId) },
                    Function { v: String? -> DelStatus.OK },
                    BiFunction<String?, Exception?, DelStatus?> { v: String?, e: Exception? ->
                        throwException(e)
                        throw RuntimeException("Unreachable code")
                    }
                )
            }

            val id: String?
                get() = dao.getId()
        }
    }

    fun mapToMultiDao(dao: RecordMutateDao?): RecordsMutateDao? {
        return object : RecordsMutateDao {
            override fun mutate(records: MutableList<RecordAtts?>): MutableList<RecordRef?>? {
                return mapElements(
                    records, Function { record: RecordAtts? -> dao.mutate(record) }, Function { obj: RecordAtts? -> obj.getId() },
                    BiFunction<RecordAtts?, Exception?, RecordRef?> { v: RecordAtts?, e: Exception? ->
                        throwException(e)
                        throw RuntimeException("Unreachable code")
                    }
                )
            }

            val id: String?
                get() = dao.getId()
        }
    }

    private fun <T, R> mapElements(input: MutableList<T?>?,
                                   mapFunc: Function<T?, R?>?,
                                   onEmpty: Function<T?, R?>?,
                                   onError: BiFunction<T?, Exception?, R?>?): MutableList<R?>? {
        val result: MutableList<R?> = ArrayList()
        for (value in input!!) {
            try {
                var res = mapFunc!!.apply(value)
                if (res == null) {
                    res = onEmpty!!.apply(value)
                }
                result.add(res)
            } catch (e: Exception) {
                OneRecDaoConverter.log.error("Mapping failed", e)
                RequestContext.Companion.getCurrentNotNull().addMsg(MsgLevel.ERROR,
                    Supplier<Any?> { ErrorUtils.convertException(e) })
                result.add(onError.apply(value, e))
            }
        }
        return result
    }
}
