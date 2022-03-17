package ru.citeck.ecos.records3.record.atts.value.factory.bean

import org.apache.commons.beanutils.PropertyUtils
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.value.AttEdge
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
import ru.citeck.ecos.records3.record.atts.value.impl.SimpleAttEdge
import java.beans.PropertyDescriptor
import java.lang.reflect.ParameterizedType

class BeanValueFactory : AttValueFactory<Any> {

    override fun getValue(value: Any): AttValue {
        return Value(value)
    }

    override fun getValueTypes() = listOf(Any::class.java)

    internal class Value(val bean: Any) : AttValue {

        private val typeCtx: BeanTypeContext = BeanTypeUtils.getTypeContext(bean.javaClass)

        override fun getId(): Any? {
            if (typeCtx.hasProperty(ScalarType.ID.schema)) {
                return getAttWithType(ScalarType.ID.schema, Any::class.java)
            }
            return null
        }

        override fun asDouble(): Double? {
            if (typeCtx.hasProperty(ScalarType.NUM.schema)) {
                return getAttWithType(ScalarType.NUM.schema, Double::class.java)
            }
            return asText()?.toDouble()
        }

        override fun asBoolean(): Boolean? {
            if (typeCtx.hasProperty(ScalarType.BOOL.schema)) {
                return getAttWithType(ScalarType.BOOL.schema, Boolean::class.java)
            }
            return asText()?.toBoolean()
        }

        override fun getType(): Any? {
            if (typeCtx.hasProperty(RecordConstants.ATT_TYPE)) {
                return getAttWithType(RecordConstants.ATT_TYPE, Any::class.java)
            }
            return null
        }

        override fun getDisplayName(): Any? {
            if (typeCtx.hasProperty(ScalarType.DISP.schema)) {
                return getAttWithType(ScalarType.DISP.schema, Any::class.java)
            }
            return asText()
        }

        override fun asText(): String? {
            if (typeCtx.hasProperty(ScalarType.STR.schema)) {
                return getAttWithType(ScalarType.STR.schema, String::class.java)
            }
            return bean.toString()
        }

        @Throws(Exception::class)
        override fun getAtt(name: String): Any? {
            if (bean is Map<*, *>) {
                return bean[name]
            }
            return typeCtx.getProperty(bean, name)
        }

        override fun has(name: String): Boolean {
            if (bean is Map<*, *>) {
                return bean.containsKey(name)
            }
            return typeCtx.beanHas(bean, name)
        }

        override fun getAs(type: String): Any? {
            return typeCtx.beanGetAs(bean, type)
        }

        override fun asJson(): Any? {
            if (typeCtx.hasProperty(ScalarType.JSON.schema)) {
                return getAttWithType(ScalarType.JSON.schema, DataValue::class.java)
            }
            return Json.mapper.toJson(bean)
        }

        override fun asRaw(): Any? {
            if (typeCtx.hasProperty(ScalarType.RAW.schema)) {
                return getAttWithType(ScalarType.RAW.schema, Any::class.java)
            }
            return Json.mapper.toJson(bean)
        }

        override fun getEdge(name: String): AttEdge {
            return typeCtx.beanGetEdge(bean, name) ?: BeanEdge(name, this)
        }

        private fun <T : Any> getAttWithType(name: String, type: Class<T>): T? {
            val value = getAtt(name)
            return if (type != Any::class.java) {
                Json.mapper.convert(getAtt(name), type)
            } else {
                @Suppress("UNCHECKED_CAST")
                value as? T
            }
        }
    }

    internal class BeanEdge(name: String, scope: Value) : SimpleAttEdge(name, scope) {

        // never executed
        private val descriptor: PropertyDescriptor? by lazy {
            try {
                PropertyUtils.getPropertyDescriptor(scope.bean, getName())
            } catch (e: NoSuchMethodException) {
                log.debug("Descriptor not found", e)
                null
            }
        }

        override fun isMultiple(): Boolean {
            val desc = descriptor ?: return false
            return Collection::class.java.isAssignableFrom(desc.propertyType)
        }

        override fun getJavaClass(): Class<*>? {
            val descriptor = descriptor ?: return null
            val type = descriptor.propertyType
            if (MutableCollection::class.java.isAssignableFrom(type)) {
                val returnType = descriptor.readMethod.genericReturnType
                val parameterType = returnType as ParameterizedType
                return parameterType.actualTypeArguments[0] as? Class<*>
            }
            return type
        }
    }
}
