package ru.citeck.ecos.records3.record.op.atts.service.value.factory.bean

import mu.KotlinLogging

class BeanTypeContext(private val getters: Map<String, (Any) -> Any?>) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    fun hasProperty(name: String): Boolean {
        return getters.containsKey(name)
    }

    @Throws(Exception::class)
    fun getProperty(bean: Any?, name: String): Any? {
        bean ?: return null
        val getter = getters[name]
        return if (getter != null) {
            getter.invoke(bean)
        } else {
            log.debug("Property not found: " + name + " in type " + bean.javaClass)
            null
        }
    }
}