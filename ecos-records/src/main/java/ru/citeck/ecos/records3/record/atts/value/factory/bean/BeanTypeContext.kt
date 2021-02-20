package ru.citeck.ecos.records3.record.atts.value.factory.bean

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import java.util.HashMap

class BeanTypeContext(
    private val getters: Map<String, (Any) -> Any?>,
    private val propsPath: Map<String, String>
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    fun hasProperty(name: String): Boolean {
        return getters.containsKey(name)
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
        return if (getter != null) {
            getter.invoke(bean)
        } else {
            log.trace("Property not found: " + name + " in type " + bean.javaClass)
            null
        }
    }
}
