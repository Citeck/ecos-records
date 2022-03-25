package ru.citeck.ecos.records3.record.atts.schema.read

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import mu.KotlinLogging
import org.apache.commons.beanutils.PropertyUtils
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.LibsUtils
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.proc.AttProcDef
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.schema.read.proc.AttWithProc
import java.beans.PropertyDescriptor
import java.lang.reflect.*
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.collections.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor

class DtoSchemaReader(factory: RecordsServiceFactory) {

    companion object {
        private val log = KotlinLogging.logger {}

        private val NULL_ATT = SchemaAtt.create()
            .withName(RecordConstants.ATT_NULL)
            .withInner(
                SchemaAtt.create()
                    .withName(ScalarType.STR.schema)
            )
            .build()
    }

    private val scalars = ConcurrentHashMap<Class<*>, ScalarField<*>>()
    private val attributesCache = ConcurrentHashMap<Class<*>, List<SchemaAtt>>()
    private val attSchemaReader = factory.attSchemaReader
    private val attProcReader = factory.attProcReader

    init {
        listOf(
            ScalarField(String::class.java, ScalarType.DISP),
            ScalarField(Boolean::class.java, ScalarType.BOOL),
            ScalarField(java.lang.Boolean::class.java, ScalarType.BOOL),
            ScalarField(Double::class.java, ScalarType.NUM),
            ScalarField(java.lang.Double::class.java, ScalarType.NUM),
            ScalarField(Float::class.java, ScalarType.NUM),
            ScalarField(java.lang.Float::class.java, ScalarType.NUM),
            ScalarField(Int::class.java, ScalarType.NUM),
            ScalarField(Integer::class.java, ScalarType.NUM),
            ScalarField(Long::class.java, ScalarType.NUM),
            ScalarField(java.lang.Long::class.java, ScalarType.NUM),
            ScalarField(Short::class.java, ScalarType.NUM),
            ScalarField(java.lang.Short::class.java, ScalarType.NUM),
            ScalarField(Byte::class.java, ScalarType.NUM),
            ScalarField(java.lang.Byte::class.java, ScalarType.NUM),
            ScalarField(Date::class.java, ScalarType.STR),
            ScalarField(Instant::class.java, ScalarType.STR),
            ScalarField(OffsetDateTime::class.java, ScalarType.STR),
            ScalarField(MLText::class.java, ScalarType.JSON),
            ScalarField(JsonNode::class.java, ScalarType.JSON),
            ScalarField(ObjectNode::class.java, ScalarType.JSON),
            ScalarField(ArrayNode::class.java, ScalarType.JSON),
            ScalarField(ObjectData::class.java, ScalarType.JSON),
            ScalarField(DataValue::class.java, ScalarType.JSON),
            ScalarField(RecordRef::class.java, ScalarType.ID),
            ScalarField(Map::class.java, ScalarType.JSON),
            ScalarField(Predicate::class.java, ScalarType.JSON)
        ).forEach(
            Consumer {
                field ->
                scalars[field.fieldType] = field
            }
        )
        if (LibsUtils.isJacksonPresent()) {
            listOf(
                ScalarField(com.fasterxml.jackson.databind.JsonNode::class.java, ScalarType.JSON),
                ScalarField(com.fasterxml.jackson.databind.node.ObjectNode::class.java, ScalarType.JSON),
                ScalarField(com.fasterxml.jackson.databind.node.ArrayNode::class.java, ScalarType.JSON)
            ).forEach(
                Consumer {
                    field ->
                    scalars[field.fieldType] = field
                }
            )
        }
    }

    fun read(attsClass: Class<*>): List<SchemaAtt> {
        return getAttributes(attsClass, null)
    }

    fun <T : Any> instantiate(attsClass: Class<T>, attributes: ObjectData): T? {
        return Json.mapper.convert(attributes, attsClass)
    }

    private fun getAttributes(attsClass: Class<*>, visited: MutableSet<Class<*>>?): List<SchemaAtt> {
        val isRoot = visited == null || visited.isEmpty()
        var attributes = attributesCache[attsClass]
        if (attributes == null) {
            attributes = getAttributesImpl(attsClass, visited ?: HashSet())
            attributesCache.putIfAbsent(attsClass, attributes)
        }
        return if (!isRoot && attributes.size == 1) {
            // prevent atts simplifying
            val newAtts = ArrayList(attributes)
            newAtts.add(NULL_ATT)
            newAtts
        } else {
            attributes
        }
    }

    private fun getAttributesImpl(attsClass: Class<*>, visited: MutableSet<Class<*>>): List<SchemaAtt> {
        require(visited.add(attsClass)) {
            (
                "Recursive attribute fields is not supported! " +
                    "Class: " + attsClass + " visited: " + visited
                )
        }

        if (Class::class.java.isAssignableFrom(attsClass)) {
            return listOf(SchemaAtt.create { name = ScalarType.STR.schema })
        }

        if (attsClass.kotlin.primaryConstructor != null) {
            val attributes = readFromConstructor(attsClass, visited)
            if (attributes.isNotEmpty()) {
                return attributes
            }
        }

        try {
            attsClass.getDeclaredConstructor()
        } catch (e: NoSuchMethodException) {
            // class without no-args constructor
            return readFromConstructor(attsClass, visited)
        }

        val descriptors: Array<PropertyDescriptor> = PropertyUtils.getPropertyDescriptors(attsClass)
        val attributes: MutableList<SchemaAtt> = ArrayList()

        for (descriptor in descriptors) {

            val writeMethod = descriptor.writeMethod ?: continue

            var propType: Class<*> = descriptor.propertyType
            var isMultiple = false
            if (List::class.java.isAssignableFrom(propType) || Set::class.java.isAssignableFrom(propType)) {
                val parameterType: ParameterizedType = writeMethod.genericParameterTypes[0] as ParameterizedType
                val type: Type = parameterType.actualTypeArguments[0]
                if (type is Class<*>) {
                    propType = parameterType.actualTypeArguments[0] as Class<*>
                } else if (type is ParameterizedType) {
                    propType = type.rawType as Class<*>
                }
                isMultiple = true
            }
            val scalarField = scalars[propType]

            getAttributeSchema(
                attsClass,
                null,
                writeMethod,
                descriptor.name,
                isMultiple,
                scalarField,
                propType,
                visited
            )?.let { attributes.add(it) }
        }
        visited.remove(attsClass)
        return attributes
    }

    private fun getParamName(param: KParameter): String? {
        return param.findAnnotation<JsonProperty>()?.value ?: param.name
    }

    private fun readFromConstructor(attsClass: Class<*>, visited: MutableSet<Class<*>>): List<SchemaAtt> {

        val kotlinClass = attsClass.kotlin

        val constructor = kotlinClass.primaryConstructor
            ?: kotlinClass.constructors.firstOrNull() ?: error("Constructor is null. Type: $attsClass")

        val args = constructor.parameters

        val atts = mutableListOf<SchemaAtt>()
        for (arg in args) {

            val paramName = getParamName(arg)
            if (paramName == null) {
                log.info { "Parameter doesn't has name: $arg and will be ignored. class: $attsClass" }
                continue
            }

            var argType = arg.type.classifier as? KClass<*> ?: error("Incorrect argument: $arg")
            var multiple = false

            if (List::class.isSuperclassOf(argType) || Set::class.isSuperclassOf(argType)) {
                multiple = true
                argType = arg.type.arguments[0].type?.classifier as? KClass<*>
                    ?: error("Incorrect collection arg: ${arg.type.arguments[0]}")
            }

            val javaClass = argType.java
            var scalar = scalars[javaClass]
            if (scalar == null && argType.isSubclassOf(Map::class)) {
                scalar = scalars[Map::class.java]
            }

            getAttributeSchema(
                attsClass,
                arg,
                null,
                paramName,
                multiple,
                scalar,
                javaClass,
                visited
            )?.let { atts.add(it) }
        }

        return atts
    }

    private fun getAttributeSchema(
        scope: Class<*>,
        argument: KParameter?,
        writeMethod: Method?,
        fieldName: String,
        multiple: Boolean,
        scalarField: ScalarField<*>?,
        propType: Class<*>,
        visited: MutableSet<Class<*>>
    ): SchemaAtt? {

        val innerAtts = if (scalarField == null) {
            if (propType.isEnum) {
                mutableListOf(
                    SchemaAtt.create()
                        .withName("?str")
                        .build()
                )
            } else {
                getAttributes(propType, visited)
            }
        } else {
            mutableListOf(
                SchemaAtt.create()
                    .withName(scalarField.scalarType.schema)
                    .build()
            )
        }
        var attNameValue: String? = null
        val attName: AttName? = getAnnotation(argument, writeMethod, scope, fieldName, AttName::class.java)
        if (attName != null) {
            attNameValue = attName.value
        } else {
            val metaAtt: MetaAtt? = getAnnotation(argument, writeMethod, scope, fieldName, MetaAtt::class.java)
            if (metaAtt != null) {
                attNameValue = metaAtt.value
            }
        }
        val att: SchemaAtt.Builder
        val processors: List<AttProcDef>
        if (StringUtils.isNotBlank(attNameValue)) {

            if (attNameValue == "...") {
                return null
            }

            val attWithProc: AttWithProc = attProcReader.read(attNameValue ?: "")
            processors = attWithProc.processors
            attNameValue = attWithProc.attribute.trim()
            val schemaAtt: SchemaAtt = attSchemaReader.readInner(fieldName, attNameValue, processors, innerAtts)
            att = schemaAtt.copy()
            if (multiple && !schemaAtt.multiple) {
                var innerAtt = schemaAtt
                while (!innerAtt.multiple && innerAtt.inner.size == 1) {
                    innerAtt = innerAtt.inner[0]
                }
                if (!innerAtt.multiple) {
                    att.withMultiple(true)
                }
            }
        } else {
            att = if (fieldName == "id" && scalarField != null) {
                if (scalarField.scalarType == ScalarType.ID) {
                    SchemaAtt.create()
                        .withMultiple(multiple)
                        .withAlias(fieldName)
                        .withName(ScalarType.ID.schema)
                } else {
                    SchemaAtt.create()
                        .withMultiple(multiple)
                        .withAlias(fieldName)
                        .withName(ScalarType.LOCAL_ID.schema)
                }
            } else {
                SchemaAtt.create()
                    .withMultiple(multiple)
                    .withAlias(fieldName)
                    .withName(fieldName)
                    .withInner(innerAtts)
            }
        }
        return att.build()
    }

    private fun <T : Annotation> getAnnotation(
        argument: KParameter?,
        writeMethod: Method?,
        scope: Class<*>,
        fieldName: String,
        type: Class<T>
    ): T? {

        var annotation = writeMethod?.getAnnotation(type)
        if (annotation == null) {
            @Suppress("UNCHECKED_CAST")
            annotation = argument?.annotations?.firstOrNull { type.isInstance(it) } as T?
        }
        if (annotation == null) {
            val field: Field?
            try {
                field = scope.getDeclaredField(fieldName)
                annotation = field.getAnnotation(type)
            } catch (e: NoSuchFieldException) {
                // ignore
            }
        }
        return annotation
    }

    private class ScalarField<FieldTypeT>(
        val fieldType: Class<FieldTypeT>,
        val scalarType: ScalarType
    )
}
