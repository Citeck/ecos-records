package ru.citeck.ecos.records3.record.atts

import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.read.AttSchemaReader
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttSchemaResolver
import ru.citeck.ecos.records3.record.atts.schema.resolver.ResolveArgs
import ru.citeck.ecos.records3.record.atts.value.impl.NullAttValue
import ru.citeck.ecos.records3.record.mixin.EmptyMixinContext
import ru.citeck.ecos.records3.record.mixin.MixinContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*
import java.util.function.Consumer
import kotlin.collections.Collection
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.emptyList
import kotlin.collections.listOf

class RecordAttsServiceImpl : RecordAttsService, ServiceFactoryAware {

    private lateinit var schemaReader: AttSchemaReader
    private lateinit var dtoSchemaReader: DtoSchemaReader
    private lateinit var schemaResolver: AttSchemaResolver
    private lateinit var services: RecordsServiceFactory

    override fun setRecordsServiceFactory(serviceFactory: RecordsServiceFactory) {
        this.schemaReader = serviceFactory.attSchemaReader
        this.dtoSchemaReader = serviceFactory.dtoSchemaReader
        this.schemaResolver = serviceFactory.attSchemaResolver
        this.services = serviceFactory
    }

    override fun <T> doWithSchema(attributes: Map<String, *>, rawAtts: Boolean, action: (List<SchemaAtt>) -> T): T {
        return doWithSchema(schemaReader.read(attributes), rawAtts, action)
    }

    override fun <T> doWithSchema(atts: List<SchemaAtt>, rawAtts: Boolean, action: (List<SchemaAtt>) -> T): T {
        return AttContext.doWithCtx(services) { attContext ->
            val schemaAttBefore = attContext.getSchemaAtt()
            val attPathBefore = attContext.getAttPath()
            if (atts.isNotEmpty()) {
                val flatAtts = schemaResolver.getFlatAttributes(atts, !rawAtts)
                attContext.setSchemaAtt(
                    SchemaAtt.create()
                        .withName(SchemaAtt.ROOT_NAME)
                        .withInner(flatAtts)
                        .build()
                )
            } else {
                attContext.setSchemaAtt(
                    SchemaAtt.create()
                        .withName(SchemaAtt.ROOT_NAME)
                        .withInner(AttContext.EMPTY)
                        .build()
                )
            }
            attContext.setAttPath("")
            try {
                action.invoke(atts)
            } finally {
                attContext.setSchemaAtt(schemaAttBefore)
                attContext.setAttPath(attPathBefore)
            }
        }
    }

    override fun <T : Any> getAtts(value: Any?, attributes: Class<T>): T {
        return getAtts(listOf(value), attributes)[0]
    }

    override fun getId(value: Any?, defaultRef: EntityRef): EntityRef {

        val id: EntityRef = getAtts(
            listOf(value ?: NullAttValue.INSTANCE),
            emptyList(),
            true,
            EmptyMixinContext,
            listOf(defaultRef)
        )[0].getId()

        return if (id.isEmpty()) {
            defaultRef
        } else {
            id.withDefault(
                appName = defaultRef.getAppName(),
                sourceId = defaultRef.getSourceId()
            )
        }
    }

    override fun getAtts(value: Any?, attributes: Map<String, String>): RecordAtts {
        return getAtts(value, attributes, false)
    }

    override fun getAtts(value: Any?, attributes: Collection<String>): RecordAtts {
        return getAtts(value, attributes, false)
    }

    override fun getAtts(value: Any?, attributes: Collection<String>, rawAtts: Boolean): RecordAtts {
        return getAtts(value, toAttsMap(attributes), rawAtts)
    }

    override fun getAtts(value: Any?, attributes: Map<String, String>, rawAtts: Boolean): RecordAtts {
        val values: List<Any?> = listOf(value)
        return getFirst(getAtts(values, attributes, rawAtts), attributes, values)
    }

    override fun getAtts(values: List<*>, attributes: Collection<String>): List<RecordAtts> {
        return getAtts(values, attributes, false)
    }

    override fun getAtts(values: List<*>, attributes: Map<String, String>): List<RecordAtts> {
        return getAtts(values, attributes, false)
    }

    override fun getAtts(values: List<*>, attributes: Collection<String>, rawAtts: Boolean): List<RecordAtts> {
        return getAtts(values, toAttsMap(attributes), rawAtts)
    }

    override fun <T : Any> getAtts(values: List<*>, attributes: Class<T>): List<T> {
        val schema = dtoSchemaReader.read(attributes)
        return getAtts(values, schema, false, EmptyMixinContext)
            .map {
                dtoSchemaReader.instantiate(attributes, it.getAtts())
                    ?: error("Attributes class can't be instantiated. Class: $attributes Schema: $schema")
            }
    }

    override fun getAtts(values: List<*>, attributes: Map<String, String>, rawAtts: Boolean): List<RecordAtts> {
        return getAtts(values, attributes, rawAtts, EmptyMixinContext)
    }

    override fun getAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        mixins: MixinContext
    ): List<RecordAtts> {
        return getAtts(values, attributes, rawAtts, mixins, emptyList())
    }

    override fun getAtts(
        values: List<*>,
        attributes: List<SchemaAtt>,
        rawAtts: Boolean,
        mixins: MixinContext,
        recordRefs: List<EntityRef>
    ): List<RecordAtts> {

        if (values.isEmpty()) {
            return emptyList()
        }

        var rootAtts: List<SchemaAtt> = attributes
        rootAtts = ArrayList(rootAtts)

        return schemaResolver.resolve(
            ResolveArgs
                .create()
                .withValues(values)
                .withAttributes(rootAtts)
                .withRawAtts(rawAtts)
                .withMixinContext(mixins)
                .withDefaultValueRefs(recordRefs)
                .build()
        )
    }

    override fun getAtts(
        values: List<*>,
        attributes: Map<String, String>,
        rawAtts: Boolean,
        mixins: MixinContext
    ): List<RecordAtts> {
        return getAtts(values, schemaReader.read(attributes), rawAtts, mixins)
    }

    private fun <T> getFirst(elements: List<T>, atts: Any?, srcValues: List<Any?>): T {
        if (elements.isEmpty()) {
            throw RuntimeException(
                "Get atts returned 0 records. " +
                    "Attributes: " + atts + " Values: " + srcValues
            )
        }
        return elements[0]
    }

    private fun toAttsMap(attributes: Collection<String>): Map<String, String> {
        val attributesMap = LinkedHashMap<String, String>()
        attributes.forEach(Consumer { att: String -> attributesMap[att] = att })
        return attributesMap
    }
}
