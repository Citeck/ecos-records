package ru.citeck.ecos.records3.record.atts.proc

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.LinkedHashMap

class AttProcService(serviceFactory: RecordsServiceFactory) {

    companion object {
        private const val PROC_ATT_ALIAS_PREFIX: String = "__proc_att_"

        private val log = KotlinLogging.logger {}
    }

    private val processors = ConcurrentHashMap<String, AttProcessor>()
    private val attSchemaReader = serviceFactory.attSchemaReader

    fun process(attributes: ObjectData, value: DataValue, processorsDef: List<AttProcDef>): DataValue? {

        if (processorsDef.isEmpty()) {
            return value
        }
        var newValue: DataValue = value
        for (def in processorsDef) {
            val proc: AttProcessor? = getProcessor(def.type)
            if (proc != null) {
                val newValueObj: Any? = proc.process(attributes, newValue, def.arguments)
                newValue = if (newValueObj is String) {
                    DataValue.createStr(newValueObj)
                } else {
                    DataValue.create(newValueObj)
                }
            }
        }
        return if (value != newValue) {
            newValue
        } else {
            value
        }
    }

    private fun getAttsToLoadSet(processorsDef: List<AttProcDef>): Set<String> {

        if (processorsDef.isEmpty()) {
            return emptySet()
        }
        val attributes: MutableSet<String> = HashSet()
        for (def in processorsDef) {
            val proc: AttProcessor? = getProcessor(def.type)
            if (proc != null) {
                attributes.addAll(proc.getAttsToLoad(def.arguments))
            }
        }
        return attributes
    }

    fun getProcessorsAtts(attributes: List<SchemaAtt>): List<SchemaAtt> {

        val attributesToLoad: MutableSet<String> = HashSet()
        for (att in attributes) {
            attributesToLoad.addAll(getAttsToLoadSet(att.processors))
        }
        if (attributesToLoad.isEmpty()) {
            return emptyList()
        }
        val procAtts: MutableMap<String, String> = LinkedHashMap()
        for (att in attributesToLoad) {
            procAtts[PROC_ATT_ALIAS_PREFIX + att] = att
        }
        return attSchemaReader.read(procAtts)
    }

    fun applyProcessors(data: ObjectData, processors: Map<String, List<AttProcDef>>): ObjectData {

        if (processors.isEmpty()) {
            return data
        }
        val dataMap: MutableMap<String, Any?> = LinkedHashMap()
        data.forEach {
            key, value ->
            dataMap[key] = value
        }
        return ObjectData.create(applyProcessors(dataMap, processors))
    }

    fun applyProcessors(data: Map<String, Any?>, processors: Map<String, List<AttProcDef>>): Map<String, Any?> {

        val procData: ObjectData = ObjectData.create()
        val resultData: MutableMap<String, Any?> = LinkedHashMap()

        data.forEach { (k, v) ->
            if (k.startsWith(PROC_ATT_ALIAS_PREFIX)) {
                procData.set(k.replaceFirst(PROC_ATT_ALIAS_PREFIX.toRegex(), ""), v)
            } else {
                resultData[k] = v
            }
        }
        if (processors.isEmpty()) {
            return resultData
        }

        processors.forEach { (att, attProcessors) ->
            if (attProcessors.isNotEmpty()) {
                val value: DataValue = DataValue.create(resultData[att])
                resultData[att] = process(procData, value, attProcessors)
            }
        }
        return resultData
    }

    private fun getProcessor(type: String): AttProcessor? {
        val proc: AttProcessor? = processors[type]
        if (proc == null) {
            log.error("Attribute processor doesn't found for type '$type'")
        }
        return proc
    }

    fun register(processor: AttProcessor) {
        processors[processor.getType()] = processor
    }
}
