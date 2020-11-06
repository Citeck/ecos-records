package ru.citeck.ecos.records3.record.op.atts.service.schema.read

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
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.op.atts.service.proc.AttProcDef
import ru.citeck.ecos.records3.record.op.atts.service.schema.ScalarType
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.proc.AttWithProc
import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class DtoSchemaReader(factory: RecordsServiceFactory) {

    companion object {
        private val log = KotlinLogging.logger {}
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
            ScalarField(java.lang.Integer::class.java, ScalarType.NUM),
            ScalarField(Long::class.java, ScalarType.NUM),
            ScalarField(java.lang.Long::class.java, ScalarType.NUM),
            ScalarField(Short::class.java, ScalarType.NUM),
            ScalarField(java.lang.Short::class.java, ScalarType.NUM),
            ScalarField(Byte::class.java, ScalarType.NUM),
            ScalarField(java.lang.Byte::class.java, ScalarType.NUM),
            ScalarField(Date::class.java, ScalarType.STR),
            ScalarField(Instant::class.java, ScalarType.STR),
            ScalarField(MLText::class.java, ScalarType.JSON),
            ScalarField(JsonNode::class.java, ScalarType.JSON),
            ScalarField(ObjectNode::class.java, ScalarType.JSON),
            ScalarField(ArrayNode::class.java, ScalarType.JSON),
            ScalarField(ObjectData::class.java, ScalarType.JSON),
            ScalarField(DataValue::class.java, ScalarType.JSON),
            ScalarField(RecordRef::class.java, ScalarType.ID),
            ScalarField(Map::class.java, ScalarType.JSON)
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

    fun <T : Any> instantiate(metaClass: Class<T>, attributes: ObjectData): T? {
        return Json.mapper.convert(attributes, metaClass)
    }

    private fun getAttributes(attsClass: Class<*>, visited: MutableSet<Class<*>>?): List<SchemaAtt> {
        var attributes = attributesCache[attsClass]
        if (attributes == null) {
            attributes = getAttributesImpl(attsClass, visited ?: HashSet())
            attributesCache.putIfAbsent(attsClass, attributes)
        }
        return attributes
    }

    private fun getAttributesImpl(attsClass: Class<*>, visited: MutableSet<Class<*>>): List<SchemaAtt> {
        require(visited.add(attsClass)) {
            (
                "Recursive meta fields is not supported! " +
                    "Class: " + attsClass + " visited: " + visited
                )
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
            attributes.add(
                getAttributeSchema(
                    attsClass,
                    writeMethod,
                    descriptor.name,
                    isMultiple,
                    scalarField,
                    propType,
                    visited
                )
            )
        }
        visited.remove(attsClass)
        return attributes
    }

    private fun getAttributeSchema(
        scope: Class<*>,
        writeMethod: Method,
        fieldName: String,
        multiple: Boolean,
        scalarField: ScalarField<*>?,
        propType: Class<*>,
        visited: MutableSet<Class<*>>
    ): SchemaAtt {

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
        val attName: AttName? = getAnnotation(writeMethod, scope, fieldName, AttName::class.java)
        if (attName != null) {
            attNameValue = attName.value
        } else {
            val metaAtt: MetaAtt? = getAnnotation(writeMethod, scope, fieldName, MetaAtt::class.java)
            if (metaAtt != null) {
                attNameValue = metaAtt.value
            }
        }
        val att: SchemaAtt.Builder
        val processors: List<AttProcDef>
        if (StringUtils.isNotBlank(attNameValue)) {
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
            att = if (fieldName == "id" && scalarField != null && scalarField.scalarType == ScalarType.DISP) {
                SchemaAtt.create()
                    .withMultiple(multiple)
                    .withAlias(fieldName)
                    .withName(ScalarType.LOCAL_ID.schema)
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
        writeMethod: Method,
        scope: Class<*>,
        fieldName: String,
        type: Class<T>
    ): T? {

        var annotation = writeMethod.getAnnotation(type)
        if (annotation == null) {
            val field: Field?
            try {
                field = scope.getDeclaredField(fieldName)
                annotation = field.getAnnotation(type)
            } catch (e: NoSuchFieldException) {
                log.error("Field not found: $fieldName", e)
            }
        }
        return annotation
    }

    private class ScalarField<FieldTypeT>(
        val fieldType: Class<FieldTypeT>,
        val scalarType: ScalarType
    )
}