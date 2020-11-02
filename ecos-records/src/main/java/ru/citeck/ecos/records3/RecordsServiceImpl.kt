package ru.citeck.ecos.records3

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.AttReadException
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*
import java.util.function.BiConsumer

class RecordsServiceImpl(private val services: RecordsServiceFactory) : AbstractRecordsService() {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val recordsResolver = services.recordsResolver
    private val attSchemaReader = services.attSchemaReader
    private val dtoSchemaReader = services.dtoSchemaReader
    private val attSchemaWriter = services.attSchemaWriter

    /* QUERY */
    override fun query(query: RecordsQuery): RecsQueryRes<RecordRef> {
        return handleRecordsQuery {
            val metaResult = recordsResolver.query(query, emptyMap<String, Any>(), true)
            metaResult.withRecords { it.getId() }
        }
    }

    override fun <T : Any> query(query: RecordsQuery, attributes: Class<T>): RecsQueryRes<T> {
        val schema = dtoSchemaReader.read(attributes)
        require(schema.isNotEmpty()) {
            "Meta class doesn't has any fields with setter. Class: $attributes"
        }
        val meta: RecsQueryRes<RecordAtts> = query(query, attSchemaWriter.writeToMap(schema))
        return meta.withRecords { dtoSchemaReader.instantiate(attributes, it.getAtts()) }
    }

    override fun query(query: RecordsQuery, attributes: Map<String, *>, rawAtts: Boolean): RecsQueryRes<RecordAtts> {
        return handleRecordsQuery { recordsResolver.query(query, attributes, rawAtts) }
    }

    /* ATTRIBUTES */
    override fun getAtts(records: Collection<*>, attributes: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {
        return handleRecordsListRead {
            recordsResolver.getAtts(ArrayList(records), attributes, rawAtts)
        }
    }

    override fun <T : Any> getAtts(records: Collection<*>, attributes: Class<T>): List<T> {

        val schema = dtoSchemaReader.read(attributes)
        if (schema.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. MetaClass: $attributes")
        }
        val attsValues = getAtts(records, attSchemaWriter.writeToMap(schema))
        return attsValues.map {
            dtoSchemaReader.instantiate(attributes, it.getAtts()) ?: attributes.newInstance()
        }
    }

    /* MUTATE */
    override fun mutate(records: List<RecordAtts>): List<RecordRef> {
        return RequestContext.doWithCtx(services) { mutateImpl(records) }
    }

    private fun mutateImpl(records: List<RecordAtts>): List<RecordRef> {

        val aliasToRecordRef = HashMap<String, RecordRef>()
        val result: MutableList<RecordRef> = ArrayList<RecordRef>()

        for (i in records.indices.reversed()) {

            val record: RecordAtts = records[i]
            val attributes = ObjectData.create()

            record.forEach(
                BiConsumer { name, valueArg ->
                    try {
                        val parsedAtt = attSchemaReader.read(name)
                        val scalarName = parsedAtt.getScalarName()
                        val value = if ("?assoc" == scalarName) {
                            convertAssocValue(valueArg, aliasToRecordRef)
                        } else {
                            valueArg
                        }
                        attributes.set(parsedAtt.name, value)
                    } catch (e: AttReadException) {
                        log.error("Attribute read failed", e)
                    }
                }
            )
            record.setAtts(attributes)

            val sourceMut: MutableList<RecordAtts> = mutableListOf(record)
            val recordMutResult = recordsResolver.mutate(sourceMut)
            if (i == 0) {
                result.add(recordMutResult[recordMutResult.size - 1])
            }
            for (resultMeta in recordMutResult) {
                val alias: String = record.getAtt(RecordConstants.ATT_ALIAS, "")
                if (ru.citeck.ecos.commons.utils.StringUtils.isNotBlank(alias)) {
                    aliasToRecordRef[alias] = resultMeta
                }
            }
        }
        return result
    }

    private fun convertAssocValue(value: DataValue, mapping: Map<String, RecordRef>): DataValue {
        if (value.isTextual()) {
            val textValue: String = value.asText()
            if (mapping.containsKey(textValue)) {
                return DataValue.create(mapping[textValue].toString())
            }
        } else if (value.isArray()) {
            val convertedValue: MutableList<DataValue?> = ArrayList<DataValue?>()
            for (node in value) {
                convertedValue.add(convertAssocValue(node, mapping))
            }
            return DataValue.create(convertedValue)
        }
        return value
    }

    override fun delete(records: List<RecordRef>): List<DelStatus> {
        return RequestContext.doWithCtx(services) { deleteImpl(records) }
    }

    private fun deleteImpl(records: List<RecordRef>): List<DelStatus> {
        return recordsResolver.delete(records)
    }

    /* OTHER */
    private fun <T : Any> handleRecordsQuery(supplier: () -> RecsQueryRes<T>): RecsQueryRes<T> {
        return RequestContext.doWithCtx(services) { ctx ->
            try {
                supplier.invoke()
            } catch (e: Throwable) {
                ctx.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
                log.error("Records resolving error", e)
                RecsQueryRes()
            }
        }
    }

    private fun <T> handleRecordsListRead(impl: () -> List<T>): List<T> {
        return try {
            RequestContext.doWithCtx(services) { impl.invoke() }
        } catch (e: Throwable) {
            log.error("Records resolving error", e)
            emptyList()
        }
    }

    override fun getSourceInfo(sourceId: String): RecordsDaoInfo? {
        return recordsResolver.getSourceInfo(sourceId)
    }

    override fun getSourcesInfo(): List<RecordsDaoInfo> {
        return recordsResolver.getSourceInfo()
    }

    override fun register(recordsSource: RecordsDao) {
        register(recordsSource.getId(), recordsSource)
    }

    override fun register(sourceId: String, recordsSource: RecordsDao) {
        recordsResolver.register(sourceId, recordsSource)
    }
}
