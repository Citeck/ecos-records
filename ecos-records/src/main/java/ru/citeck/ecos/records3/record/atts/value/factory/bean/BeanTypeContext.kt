package ru.citeck.ecos.records3.record.atts.value.factory.bean

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.atts.value.AttEdge
import java.lang.reflect.InvocationTargetException
import java.util.HashMap

class BeanTypeContext(
    private val getters: Map<String, (Any) -> Any?>,
    private val propsPath: Map<String, String>,
    private val getAsMethod: ((Any, String) -> Any?)?,
    private val hasMethod: ((Any, String) -> Boolean)?,
    private val getEdgeMethod: ((Any, String) -> AttEdge?)?,
    private val getAttMethod: ((Any, String) -> Any?)?,
    private val getAsTextMethod: (Any) -> String?
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    fun hasProperty(name: String): Boolean {
        return getters.containsKey(name)
    }

    fun beanGetAsText(value: Any): String? {
        return doWithoutInvocationTargetException {
            getAsTextMethod.invoke(value)
        }
    }

    fun beanGetAs(value: Any, arg: String): Any? {
        return doWithoutInvocationTargetException {
            getAsMethod?.invoke(value, arg)
        }
    }

    fun beanHas(value: Any, name: String): Boolean {
        return doWithoutInvocationTargetException {
            hasMethod?.invoke(value, name) ?: hasProperty(name)
        }
    }

    fun beanGetEdge(value: Any, name: String): AttEdge? {
        return doWithoutInvocationTargetException {
            getEdgeMethod?.invoke(value, name)
        }
    }

    fun applyData(bean: Any, data: ObjectData) {

        var dataForBean = data

        if (propsPath.isNotEmpty()) {
            val newAtts = HashMap<String, DataValue>()
            val newInnerAtts = HashMap<String, MutableMap<String, DataValue>>()
            data.forEach { k, v ->
                val prop = propsPath[k]
                if (prop != null) {
                    newInnerAtts.computeIfAbsent(prop) { HashMap<String, DataValue>() }[k] = v
                } else {
                    newAtts[k] = v
                }
            }
            newInnerAtts.forEach { (k, v) ->
                newAtts[k] = DataValue.create(v)
            }
            dataForBean = ObjectData.create(newAtts)
        }

        Json.mapper.applyData(bean, dataForBean)
    }

    @Throws(Exception::class)
    fun getProperty(bean: Any?, name: String): Any? {
        bean ?: return null
        val getter = getters[name]
        return doWithoutInvocationTargetException {
            if (getter != null) {
                getter.invoke(bean)
            } else {
                if (getAttMethod != null) {
                    return getAttMethod.invoke(bean, name)
                } else {
                    log.trace("Property not found: " + name + " in type " + bean.javaClass)
                }
                null
            }
        }
    }

    private inline fun <T> doWithoutInvocationTargetException(action: () -> T): T {
        return try {
            action.invoke()
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }
}
