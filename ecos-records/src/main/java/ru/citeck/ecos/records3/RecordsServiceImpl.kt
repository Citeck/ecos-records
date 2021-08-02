package ru.citeck.ecos.records3

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.read.AttReadException
import ru.citeck.ecos.records3.record.dao.HasSourceIdAliases
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*
import kotlin.collections.ArrayList

class RecordsServiceImpl(private val services: RecordsServiceFactory) : AbstractRecordsService() {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val recordsResolver = services.recordsResolver
    private val attSchemaReader = services.attSchemaReader
    private val dtoSchemaReader = services.dtoSchemaReader
    private val attSchemaWriter = services.attSchemaWriter
    private val isGatewayMode = services.properties.gatewayMode

    init {
        recordsResolver.setRecordsService(this)
    }

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
            "Attributes class doesn't have any fields with setter. Class: $attributes"
        }
        val meta: RecsQueryRes<RecordAtts> = query(query, attSchemaWriter.writeToMap(schema))
        return meta.withRecords { dtoSchemaReader.instantiate(attributes, it.getAtts()) }
    }

    override fun query(query: RecordsQuery, attributes: Map<String, *>, rawAtts: Boolean): RecsQueryRes<RecordAtts> {
        return handleRecordsQuery { recordsResolver.query(query, attributes, rawAtts) }
    }

    /* ATTRIBUTES */
    override fun getAtts(records: Collection<*>, attributes: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {
        return RequestContext.doWithCtx(services, { ctx -> ctx.withReadOnly(true) }) { ctx ->
            try {
                recordsResolver.getAtts(ArrayList(records), attributes, rawAtts)
            } catch (e: Throwable) {
                if (ctx.ctxData.omitErrors) {
                    ctx.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }

                    val emptyAtts = ObjectData.create()
                    attributes.keys.forEach { emptyAtts.set(it, DataValue.NULL) }

                    val result = ArrayList<RecordAtts>(records.size)
                    for (record in records) {
                        result.add(RecordAtts(tryToGetRecordRef(record), emptyAtts.deepCopy()))
                    }
                    result
                } else {
                    throw e
                }
            }
        }
    }

    private fun tryToGetRecordRef(record: Any?): RecordRef {
        record ?: return RecordRef.EMPTY
        if (record is RecordRef) {
            return record
        }
        return RecordRef.EMPTY
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

        if (isGatewayMode) {
            return recordsResolver.mutate(records)
        }

        val aliasToRecordRef = HashMap<String, RecordRef>()
        val result = Array(records.size) { RecordRef.EMPTY }

        val context = RequestContext.getCurrentNotNull()
        if (context.ctxData.readOnly) {
            error("Mutation is not allowed in read-only mode. Records: " + records.map { it.getId() })
        }
        val txnMutRecords = context.getTxnChangedRecords()

        for (i in records.indices.reversed()) {

            val record: RecordAtts = records[i]
            val attributes = ObjectData.create()

            record.forEach { name, valueArg ->
                try {
                    val parsedAtt = attSchemaReader.read("", name)
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
            record.setAtts(attributes)

            val sourceMut: MutableList<RecordAtts> = mutableListOf(record)
            val recordMutResult = recordsResolver.mutate(sourceMut)

            val resultRef = recordMutResult.last()
            result[i] = resultRef
            if (RecordRef.isNotEmpty(resultRef)) {
                txnMutRecords?.add(resultRef)
            }

            for (resultMeta in recordMutResult) {
                val alias: String = record.getAtt(RecordConstants.ATT_ALIAS, "")
                if (ru.citeck.ecos.commons.utils.StringUtils.isNotBlank(alias)) {
                    aliasToRecordRef[alias] = resultMeta
                }
            }
        }
        return result.toList()
    }

    private fun convertAssocValue(value: DataValue, mapping: Map<String, RecordRef>): DataValue {
        if (value.isTextual()) {
            val textValue: String = value.asText()
            if (mapping.containsKey(textValue)) {
                return DataValue.create(mapping[textValue].toString())
            }
        } else if (value.isArray()) {
            val convertedValue: MutableList<DataValue?> = ArrayList()
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
        val context = RequestContext.getCurrentNotNull()
        if (context.ctxData.readOnly) {
            error("Deletion is not allowed in read-only mode. Records: $records")
        }
        val status = recordsResolver.delete(records)
        context.getTxnChangedRecords()?.addAll(records)
        return status
    }

    /* OTHER */
    private fun <T : Any> handleRecordsQuery(supplier: () -> RecsQueryRes<T>): RecsQueryRes<T> {
        return RequestContext.doWithCtx(services, { ctx -> ctx.withReadOnly(true) }) { ctx ->
            try {
                supplier.invoke()
            } catch (e: Throwable) {
                if (ctx.ctxData.omitErrors) {
                    ctx.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
                    RecsQueryRes()
                } else {
                    throw e
                }
            }
        }
    }

    override fun register(recordsSource: RecordsDao) {
        register(recordsSource.getId(), recordsSource)
        if (recordsSource is HasSourceIdAliases) {
            recordsSource.getSourceIdAliases().forEach {
                if (recordsResolver.getSourceInfo(it) == null) {
                    register(it, recordsSource)
                }
            }
        }
    }

    override fun register(sourceId: String, recordsSource: RecordsDao) {
        recordsResolver.register(sourceId, recordsSource)
    }

    override fun unregister(sourceId: String) {
        recordsResolver.unregister(sourceId)
    }

    override fun <T : Any> getRecordsDao(sourceId: String, type: Class<T>): T? {
        return recordsResolver.getRecordsDao(sourceId, type)
    }
}
