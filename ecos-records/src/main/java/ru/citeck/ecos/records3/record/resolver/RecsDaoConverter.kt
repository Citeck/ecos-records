package ru.citeck.ecos.records3.record.resolver

import ecos.com.fasterxml.jackson210.databind.node.ArrayNode
import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.ReflectUtils
import ru.citeck.ecos.records2.RecordRef
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
import java.util.*

class RecsDaoConverter(
    private val currentAppName: String
) {

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
        if (targetType == RecordMutateWithAnyResDao::class.java) {
            when (dao) {
                is RecordMutateDao -> {
                    return mapToMutateWithAnyResDao(dao)
                }
                is RecordsMutateWithAnyResDao -> {
                    return mapToMutateWithAnyResDao(dao)
                }
                is RecordsMutateDao -> {
                    return mapToMutateWithAnyResDao(dao)
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

    private fun mapValueMutateDaoToAnyResDao(dao: ValueMutateDao<Any>): RecordMutateWithAnyResDao {

        val valueType = ReflectUtils.getGenericArg(dao::class.java, ValueMutateDao::class.java)
            ?: error("Generic type <T> is not found for class ${dao::class.java}")

        val prepareArgument: (LocalRecordAtts) -> Any = when (valueType) {
            RecordRef::class.java -> {
                { it.id }
            }
            LocalRecordAtts::class.java -> {
                { it }
            }
            ObjectData::class.java -> {
                { it.attributes }
            }
            DataValue::class.java -> {
                { it.attributes.getData() }
            }
            else -> {
                {
                    it.attributes.getAs(valueType)
                        ?: error("Attributes conversion failed. Type: $valueType DAO: ${dao::class.java}")
                }
            }
        }

        return object : RecordMutateWithAnyResDao {
            override fun getId() = dao.getId()
            override fun mutateForAnyRes(record: LocalRecordAtts): Any? {
                return dao.mutate(prepareArgument.invoke(record))
            }
        }
    }

    private fun mapToMutateWithAnyResDao(dao: RecordsMutateCrossSrcDao): RecordMutateWithAnyResDao {
        return object : RecordMutateWithAnyResDao {
            override fun getId() = dao.getId()
            override fun mutateForAnyRes(record: LocalRecordAtts): Any? {
                return dao.mutate(listOf(record)).firstOrNull()
            }
        }
    }

    private fun mapToMutateWithAnyResDao(dao: RecordsMutateWithAnyResDao): RecordMutateWithAnyResDao {
        return object : RecordMutateWithAnyResDao {
            override fun getId() = dao.getId()
            override fun mutateForAnyRes(record: LocalRecordAtts): Any? {
                return dao.mutateForAnyRes(listOf(record)).firstOrNull()
            }
        }
    }

    private fun mapToMutateWithAnyResDao(dao: RecordMutateDao): RecordMutateWithAnyResDao {
        return object : RecordMutateWithAnyResDao {
            override fun getId() = dao.getId()
            override fun mutateForAnyRes(record: LocalRecordAtts): Any {
                return dao.mutate(record)
            }
        }
    }

    private fun mapToMutateWithAnyResDao(dao: RecordsMutateDao): RecordMutateWithAnyResDao {
        return object : RecordMutateWithAnyResDao {
            override fun getId() = dao.getId()
            override fun mutateForAnyRes(record: LocalRecordAtts): Any? {
                return dao.mutate(listOf(record)).firstOrNull()
            }
        }
    }

    private fun mapToRecordsQueryResDao(dao: RecordsQueryDao): RecordsQueryResDao {
        return object : RecordsQueryResDao {

            override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
                val records = dao.queryRecords(recsQuery) ?: return null
                if (records is RecsQueryRes<*>) {
                    return records
                }
                if (records is Collection<*>) {
                    val daoId = dao.getId()
                    val (appName, sourceId) = if (daoId.contains('/')) {
                        val appWithSrc = daoId.split("/")
                        appWithSrc[0] to appWithSrc[1]
                    } else {
                        currentAppName to daoId
                    }
                    val resRecs = records.mapNotNull {
                        if (it is String) {
                            RecordRef.valueOf(it).withDefault(
                                appName = appName,
                                sourceId = sourceId
                            )
                        } else {
                            it
                        }
                    }
                    val result = RecsQueryRes<Any>()
                    result.setRecords(resRecs)
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

            override fun getRecordsAtts(recordIds: List<String>): List<*> {
                return mapElements(
                    recordIds,
                    { dao.getRecordAtts(it) },
                    { EmptyAttValue.INSTANCE }
                )
            }
            override fun getId(): String = dao.getId()
        }
    }

    private fun mapToMultiDao(dao: RecordDeleteDao): RecordsDeleteDao {

        return object : RecordsDeleteDao {

            override fun delete(recordIds: List<String>): List<DelStatus> {
                return mapElements(
                    recordIds,
                    { dao.delete(it) },
                    { DelStatus.OK }
                )
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
        onEmpty: (T) -> R
    ): List<R> {

        val result: MutableList<R> = ArrayList()
        for (value in input) {
            var res = mapFunc.invoke(value)
            if (res == null) {
                res = onEmpty.invoke(value)
            }
            result.add(res)
        }
        return result
    }
}
