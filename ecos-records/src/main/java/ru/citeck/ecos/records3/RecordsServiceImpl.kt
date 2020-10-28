package ru.citeck.ecos.records3

import mu.KotlinLogging
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.RecordsServiceFactory
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import java.util.*
import java.util.function.Function
import java.util.function.Supplier

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

    override fun <T: Any> query(query: RecordsQuery, attributes: Class<T>): RecsQueryRes<T> {
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

    override fun <T> getAtts(records: MutableCollection<*>, metaClass: Class<T?>): MutableList<T?> {
        val attributes: MutableList<SchemaAtt?> = dtoAttsSchemaReader.read(metaClass)
        if (attributes.isEmpty()) {
            RecordsServiceImpl.log.warn("Attributes is empty. Query will return empty meta. MetaClass: $metaClass")
        }
        val meta: MutableList<RecordAtts> = getAtts(records, attSchemaWriter.writeToMap(attributes))
        return meta.stream()
            .map(Function<RecordAtts, T?> { m: RecordAtts -> dtoAttsSchemaReader.instantiate(metaClass, m.getAtts()) })
            .collect(Collectors.toList())
    }

    /* MUTATE */
    override fun mutate(records: MutableList<RecordAtts>): MutableList<RecordRef> {
        return RequestContext.Companion.doWithCtx(serviceFactory, Function<RequestContext?, MutableList<RecordRef>?> { ctx: RequestContext? -> mutateImpl(records) })
    }

    private fun mutateImpl(records: MutableList<RecordAtts>): MutableList<RecordRef> {
        val aliasToRecordRef: MutableMap<String?, RecordRef> = HashMap<String?, RecordRef>()
        val result: MutableList<RecordRef> = ArrayList<RecordRef>()
        for (i in records.indices.reversed()) {
            val record: RecordAtts = records[i]
            val attributes: ObjectData = ObjectData.create()
            record.forEach(BiConsumer<String?, DataValue?> { name: String?, value: DataValue? ->
                try {
                    val parsedAtt: SchemaAtt = attSchemaReader.read(name)
                    val scalarName: String = parsedAtt.getScalarName()
                    if ("?assoc" == scalarName) {
                        value = convertAssocValue(value, aliasToRecordRef)
                    }
                    attributes.set(parsedAtt.name, value)
                } catch (e: AttReadException) {
                    RecordsServiceImpl.log.error("Attribute read failed", e)
                }
            })
            record.setAtts(attributes)
            val sourceMut: MutableList<RecordAtts> = listOf(record)
            var recordMutResult: MutableList<RecordRef> = recordsResolver.mutate(sourceMut)
            if (recordMutResult == null) {
                recordMutResult = sourceMut.stream()
                    .map(Function<RecordAtts, RecordRef> { obj: RecordAtts -> obj.getId() })
                    .collect(Collectors.toList())
            }
            if (i == 0) {
                result.add(recordMutResult[recordMutResult.size - 1])
            }
            for (resultMeta in recordMutResult) {
                val alias: String = record.getAtt(RecordConstants.ATT_ALIAS, "")
                if (isNotBlank(alias)) {
                    aliasToRecordRef[alias] = resultMeta
                }
            }
        }
        return result
    }

    private fun convertAssocValue(value: DataValue?, mapping: MutableMap<String?, RecordRef>?): DataValue? {
        if (value.isTextual()) {
            val textValue: String = value.asText()
            if (mapping!!.containsKey(textValue)) {
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

    override fun delete(records: MutableList<RecordRef>): MutableList<DelStatus?> {
        return RequestContext.Companion.doWithCtx(serviceFactory, Function<RequestContext?, MutableList<DelStatus?>?> { ctx: RequestContext? -> deleteImpl(records) })
    }

    private fun deleteImpl(records: MutableList<RecordRef>): MutableList<DelStatus?> {
        var result: MutableList<DelStatus?>? = recordsResolver.delete(records)
        if (result == null) {
            result = ArrayList<DelStatus?>(records.size)
            for (i in records.indices) {
                result!!.add(DelStatus.OK)
            }
        }
        return result!!
    }

    /* OTHER */
    private fun <T: Any> handleRecordsQuery(supplier: () -> RecsQueryRes<T>): RecsQueryRes<T> {
        return RequestContext.doWithCtx(services, { ctx ->
            try {
                supplier.invoke()
            } catch (e: Throwable) {
                ctx.addMsg(MsgLevel.ERROR) { ErrorUtils.convertException(e) }
                log.error("Records resolving error", e)
                RecsQueryRes()
            }
        })
    }

    private fun <T> handleRecordsListRead(impl: Supplier<MutableList<T?>?>?): MutableList<T?>? {
        var result: MutableList<T?>?
        try {
            result = RequestContext.Companion.doWithCtx(serviceFactory, Function { ctx: RequestContext? -> impl!!.get() })
        } catch (e: Throwable) {
            RecordsServiceImpl.log.error("Records resolving error", e)
            result = emptyList()
        }
        return result
    }

    override fun getSourceInfo(sourceId: String?): RecordsDaoInfo? {
        return if (sourceId == null) {
            null
        } else recordsResolver.getSourceInfo(sourceId)
    }

    override fun getSourcesInfo(): MutableList<RecordsDaoInfo?> {
        return recordsResolver.getSourceInfo()
    }

    override fun register(recordsSource: RecordsDao) {
        register(recordsSource.getId(), recordsSource)
    }

    override fun register(sourceId: String, recordsSource: RecordsDao) {
        recordsResolver.register(sourceId, recordsSource)
    }
}
