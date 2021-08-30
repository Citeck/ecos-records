package ru.citeck.ecos.records3.record.resolver

import ecos.com.fasterxml.jackson210.databind.node.ArrayNode
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.ReflectUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.value.factory.bean.BeanTypeUtils
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.*
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryResDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*

class RecsDaoConverter {

    companion object {
        val log = KotlinLogging.logger {}
    }

    fun convert(dao: RecordsDao, targetType: Class<*>): RecordsDao {

        if (targetType.isInstance(dao)) {
            return dao
        }

        if (dao is RecordsQueryDao && targetType == RecordsQueryResDao::class.java) {
            return mapToRecordsQueryResDao(dao)
        }
        if (dao is RecordAttsDao && targetType == RecordsAttsDao::class.java) {
            return mapToMultiDao(dao)
        }
        if (dao is RecordDeleteDao && targetType == RecordsDeleteDao::class.java) {
            return mapToMultiDao(dao)
        }
        if (targetType == RecordsMutateWithAnyResDao::class.java) {
            when (dao) {
                is RecordMutateDao -> {
                    return mapToMutateWithAnyResDao(mapToMultiDao(dao))
                }
                is RecordsMutateDao -> {
                    return mapToMutateWithAnyResDao(mapToMultiDao(dao))
                }
                is RecordMutateDtoDao<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    return mapToMutateWithAnyResDao(mapToMutateCrossSrc(dao as RecordMutateDtoDao<Any>))
                }
                is RecordsMutateCrossSrcDao -> {
                    return mapToMutateWithAnyResDao(dao)
                }
                is ValueMutateDao<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    return mapValueMutateDaoToAnyResDao(dao as ValueMutateDao<Any>)
                }
            }
        }
        return dao
    }

    private fun mapValueMutateDaoToAnyResDao(dao: ValueMutateDao<Any>): RecordsMutateWithAnyResDao {

        val valueType = ReflectUtils.getGenericArg(dao::class.java, ValueMutateDao::class.java)
            ?: error("Generic type <T> is not found for class ${dao::class.java}")

        val convert: (LocalRecordAtts) -> Any = when (valueType) {
            RecordRef::class.java -> { { it.id } }
            LocalRecordAtts::class.java -> { { it } }
            ObjectData::class.java -> { { it.attributes } }
            DataValue::class.java -> { { it.attributes.getData() } }
            else -> {
                {
                    it.attributes.getAs(valueType)
                        ?: error("Attributes conversion failed. Type: $valueType DAO: ${dao::class.java}")
                }
            }
        }

        return object : RecordsMutateWithAnyResDao {
            override fun getId() = dao.getId()
            override fun mutate(records: List<LocalRecordAtts>): List<Any> {
                if (records.isEmpty()) {
                    return emptyList()
                }
                val arg = convert.invoke(records[0])
                return listOf(dao.mutate(arg) ?: EmptyAttValue.INSTANCE)
            }
        }
    }

    private fun mapToMutateWithAnyResDao(dao: RecordsMutateCrossSrcDao): RecordsMutateWithAnyResDao {
        return object : RecordsMutateWithAnyResDao {
            override fun getId() = dao.getId()
            override fun mutate(records: List<LocalRecordAtts>): List<Any> {
                return dao.mutate(records)
            }
        }
    }

    private fun mapToRecordsQueryResDao(dao: RecordsQueryDao): RecordsQueryResDao {
        return object : RecordsQueryResDao {

            override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
                var records = dao.queryRecords(recsQuery) ?: return null
                if (records is RecsQueryRes<*>) {
                    return records
                }
                if (records is Set<*>) {
                    records = ArrayList(records)
                }
                if (records is List<*>) {
                    val result = RecsQueryRes<Any>()
                    result.setRecords(records)
                    return result
                }
                if (records is DataValue && records.isArray()) {
                    val result = RecsQueryRes<Any>()
                    result.setRecords(records.asList(DataValue::class.java))
                    return result
                }
                if (records is ArrayNode) {
                    val result = RecsQueryRes<Any>()
                    result.setRecords(DataValue.create(records).asList(DataValue::class.java))
                    return result
                }
                return RecsQueryRes(listOf(records))
            }

            override fun getId() = dao.getId()
        }
    }

    private fun mapToMultiDao(dao: RecordAttsDao): RecordsAttsDao {

        return object : RecordsAttsDao {

            override fun getRecordsAtts(recordsId: List<String>): List<*> {
                return mapElements(
                    recordsId,
                    { dao.getRecordAtts(it) },
                    { EmptyAttValue.INSTANCE },
                    { _, _ -> ObjectData.create() }
                )
            }
            override fun getId(): String = dao.getId()
        }
    }

    private fun mapToMultiDao(dao: RecordDeleteDao): RecordsDeleteDao {

        return object : RecordsDeleteDao {

            override fun delete(recordsId: List<String>): List<DelStatus> {
                return mapElements(
                    recordsId,
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

                    val ctx = BeanTypeUtils.getTypeContext(record::class.java)
                    ctx.applyData(record, it.attributes)

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
