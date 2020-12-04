package ru.citeck.ecos.records3.record.op.atts.service.value.factory.bean

import org.apache.commons.beanutils.PropertyUtils
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.op.atts.service.value.factory.AttValueFactory
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.SimpleAttEdge
import java.beans.PropertyDescriptor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

class BeanValueFactory : AttValueFactory<Any> {

    override fun getValue(value: Any): AttValue {
        return Value(value)
    }

    override fun getValueTypes() = listOf(Any::class.java)

    internal class Value(val bean: Any) : AttValue {

        private val typeCtx: BeanTypeContext = BeanTypeUtils.getTypeContext(bean.javaClass)

        override fun getId(): Any? {
            return if (typeCtx.hasProperty("?id")) {
                getAttWithType("?id", Any::class.java)
            } else try {
                PropertyUtils.getProperty(bean, "id")
            } catch (e: NoSuchMethodException) {
                null
            } catch (e: IllegalAccessException) {
                null
            } catch (e: InvocationTargetException) {
                null
            }
        }

        override fun asDouble(): Double? {
            if (typeCtx.hasProperty("?num")) {
                return getAttWithType("?num", Double::class.java)
            }
            return asText()?.toDouble()
        }

        override fun asBoolean(): Boolean? {
            if (typeCtx.hasProperty("?bool")) {
                return getAttWithType("?bool", Boolean::class.java)
            }
            return asText()?.toBoolean()
        }

        override fun getType(): RecordRef {
            if (typeCtx.hasProperty(RecordConstants.ATT_TYPE)) {
                return getAttWithType(RecordConstants.ATT_TYPE, RecordRef::class.java) ?: RecordRef.EMPTY
            }
            return RecordRef.EMPTY
        }

        override fun getDisplayName(): String? {
            if (typeCtx.hasProperty("?disp")) {
                return getAttWithType("?disp", String::class.java)
            }
            return asText()
        }

        override fun asText(): String? {
            if (typeCtx.hasProperty("?str")) {
                return getAttWithType("?str", String::class.java)
            }
            return bean.toString()
        }

        @Throws(Exception::class)
        override fun getAtt(name: String): Any? {
            if (bean is MutableMap<*, *>) {
                return bean[name]
            }
            return typeCtx.getProperty(bean, name)
        }

        override fun has(name: String): Boolean {
            return typeCtx.hasProperty(name)
        }

        override fun asJson(): Any? {
            if (typeCtx.hasProperty("?json")) {
                return getAttWithType("?json", DataValue::class.java)
            }
            return Json.mapper.toJson(bean)
        }

        override fun getEdge(name: String): AttEdge? {
            return BeanEdge(name, this)
        }

        private fun <T : Any> getAttWithType(name: String, type: Class<T>): T? {
            return try {
                val value = getAtt(name)
                return if (type != Any::class.java) {
                    Json.mapper.convert(getAtt(name), type)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    value as? T
                }
            } catch (e: Exception) {
                null
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
