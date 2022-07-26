package ru.citeck.ecos.records3

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.request.error.ErrorUtils
import ru.citeck.ecos.records2.utils.ValWithIdx
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.read.AttReadException
import ru.citeck.ecos.records3.record.dao.HasSourceIdAliases
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.records3.record.request.msg.MsgLevel
import ru.citeck.ecos.records3.utils.RecordRefUtils
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class RecordsServiceImpl(private val services: RecordsServiceFactory) : AbstractRecordsService() {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val recordsResolver = services.recordsResolver
    private val attSchemaReader = services.attSchemaReader
    private val dtoSchemaReader = services.dtoSchemaReader
    private val attSchemaWriter = services.attSchemaWriter

    private val isGatewayMode = services.properties.gatewayMode
    private val currentAppName = services.properties.appName
    private val defaultAppName = services.properties.defaultApp
    private val legacyApiMode = services.properties.legacyApiMode

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
            dtoSchemaReader.instantiate(attributes, it.getAtts())
                ?: error("Attributes class can't be instantiated. Class: $attributes Schema: $schema")
        }
    }

    /* MUTATE */

    override fun mutate(records: List<RecordAtts>, attsToLoad: Map<String, *>, rawAtts: Boolean): List<RecordAtts> {
        return RequestContext.doWithCtx(services) {
            if (records.isEmpty()) {
                emptyList()
            } else {
                val context = RequestContext.getCurrentNotNull()
                if (context.ctxData.readOnly) {
                    error("Mutation is not allowed in read-only mode. Records: " + records.map { it.getId() })
                }
                mutateForAllApps(records.map { it.deepCopy() }, attsToLoad, rawAtts, context)
            }
        }
    }

    override fun <T : Any> mutate(record: Any, attributes: Any, attsToLoad: Class<T>): T {
        val schema = dtoSchemaReader.read(attsToLoad)
        require(schema.isNotEmpty()) {
            "Attributes class doesn't have any fields with setter. Class: $attributes"
        }
        val meta = mutate(record, attributes, attSchemaWriter.writeToMap(schema))
        return dtoSchemaReader.instantiate(attsToLoad, meta.getAtts())
            ?: error("Attributes class can't be instantiated. Class: $attsToLoad Schema: $schema")
    }

    private inline fun <T> addTxnMutatedRecords(
        txnChangedRecords: MutableSet<RecordRef>?,
        sourceIdMapping: Map<String, String>,
        records: List<T>,
        getRef: (T) -> RecordRef
    ) {
        if (isGatewayMode || txnChangedRecords == null) {
            return
        }
        records.forEach {
            addTxnMutatedRecord(txnChangedRecords, sourceIdMapping, getRef.invoke(it))
        }
    }

    private fun addTxnMutatedRecord(
        txnChangedRecords: MutableSet<RecordRef>?,
        sourceIdMapping: Map<String, String>,
        recordRef: RecordRef?
    ) {

        if (isGatewayMode || txnChangedRecords == null || recordRef == null || RecordRef.isEmpty(recordRef)) {
            return
        }
        txnChangedRecords.add(
            RecordRefUtils.mapAppIdAndSourceId(
                recordRef,
                currentAppName,
                sourceIdMapping
            )
        )
    }

    private fun mutateForApp(
        appName: String,
        records: List<RecordAtts>,
        attsToLoad: Map<String, *>,
        rawAtts: Boolean,
        context: RequestContext
    ): List<RecordAtts> {

        val txnChangedRecords = context.getTxnChangedRecords()
        val sourceIdMapping = context.ctxData.sourceIdMapping

        if (currentAppName.isNotEmpty() && currentAppName != appName) {
            val result = recordsResolver.mutate(records, attsToLoad, rawAtts)
            addTxnMutatedRecords(txnChangedRecords, sourceIdMapping, result) { it.getId() }
            return result
        }

        val aliasToRecordRef = HashMap<String, RecordRef>()

        val emptyRecAtts = RecordAtts()
        val result = Array(records.size) { emptyRecAtts }

        for (i in records.indices.reversed()) {

            val record: RecordAtts = records[i]
            convertAssocValues(record, aliasToRecordRef, true)

            val sourceMut: MutableList<RecordAtts> = mutableListOf(record)
            val recordMutResult = recordsResolver.mutate(sourceMut, attsToLoad, rawAtts)

            val resultAtts = recordMutResult.last()
            result[i] = resultAtts
            addTxnMutatedRecord(txnChangedRecords, sourceIdMapping, resultAtts.getId())

            for (resultMeta in recordMutResult) {
                val alias: String = record.getAtt(RecordConstants.ATT_ALIAS, "")

                if (StringUtils.isNotBlank(alias)) {
                    aliasToRecordRef[alias] = resultMeta.getId()
                }
            }
        }
        return result.toList()
    }

    private fun mutateForAllApps(
        records: List<RecordAtts>,
        attsToLoad: Map<String, *>,
        rawAtts: Boolean,
        context: RequestContext
    ): List<RecordAtts> {

        if (records.size == 1) {
            val appName = getAppName(records[0].getId())
            return mutateForApp(
                appName,
                records,
                attsToLoad,
                rawAtts,
                context
            )
        }

        val recsToMutate = ArrayList<ValWithIdx<RecordAtts>>()
        val allRecsAfterMutate = ArrayList<ValWithIdx<RecordAtts>>()
        val refsByAliases = HashMap<String, RecordRef>()

        var appToMutate = ""

        val flushRecords = {

            if (refsByAliases.isNotEmpty()) {
                for (record in recsToMutate) {
                    convertAssocValues(record.value, refsByAliases, false)
                }
            }
            recsToMutate.reverse()
            val recsAfterMutate = mutateForApp(
                appToMutate,
                recsToMutate.map { it.value },
                attsToLoad,
                rawAtts,
                context
            )

            for ((idx, atts) in recsAfterMutate.withIndex()) {
                val recToMutateWithIdx = recsToMutate[idx]
                val alias = findAliasInRawAttsToMutate(recToMutateWithIdx.value.getAtts())
                if (alias.isNotBlank()) {
                    refsByAliases[alias] = atts.getId()
                }
                allRecsAfterMutate.add(ValWithIdx(atts, recToMutateWithIdx.idx))
            }
            recsToMutate.clear()
        }

        for (i in records.indices.reversed()) {
            val record = records[i]
            val appName = getAppName(record.getId())
            if (appToMutate.isEmpty() || (appName == appToMutate && !legacyApiMode)) {
                appToMutate = appName
                recsToMutate.add(ValWithIdx(record, i))
            } else {
                flushRecords()
                appToMutate = appName
                recsToMutate.add(ValWithIdx(record, i))
            }
        }
        if (recsToMutate.isNotEmpty()) {
            flushRecords()
        }

        allRecsAfterMutate.sortBy { it.idx }
        return allRecsAfterMutate.map { it.value }
    }

    private fun getAppName(ref: RecordRef): String {
        return ref.appName.ifEmpty {
            if (isGatewayMode) {
                defaultAppName
            } else {
                currentAppName
            }
        }
    }

    private fun findAliasInRawAttsToMutate(rawAtts: ObjectData): String {
        if (rawAtts.size() == 0) {
            return ""
        }
        for (field in rawAtts.fieldNames()) {
            if (field.startsWith(RecordConstants.ATT_ALIAS)) {
                if (RecordConstants.ATT_ALIAS == field.substringBefore('?')) {
                    return rawAtts.get(field, "")
                }
            }
        }
        return ""
    }

    private fun convertAssocValues(record: RecordAtts, assocsMapping: Map<String, RecordRef>, simplifyAtts: Boolean) {

        if (assocsMapping.isEmpty() && !simplifyAtts) {
            return
        }

        val recAtts = ObjectData.create()

        record.forEach { name, valueArg ->
            try {
                val parsedAtt = attSchemaReader.read("", name)
                recAtts.set(parsedAtt.name, convertAssocValue(valueArg, assocsMapping))
            } catch (e: AttReadException) {
                log.error("Attribute read failed", e)
            }
        }
        record.setAtts(recAtts)
    }

    private fun convertAssocValue(value: DataValue, mapping: Map<String, RecordRef>): DataValue {
        if (mapping.isEmpty()) {
            return value
        }
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
