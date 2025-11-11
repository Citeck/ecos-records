package ru.citeck.ecos.records3.record.atts.value

import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.mime.MimeType

class AttValuesConverter(private val services: RecordsServiceFactory) {

    private val factoriesByType = HashMap<Class<out AttValueFactory<*>>, AttValueFactory<*>>()
    private val factoriesByInterface = HashMap<Class<*>, AttValueFactory<Any>>()

    private val valueFactories: Map<Class<*>, AttValueFactory<Any>> by lazy {
        val valueFactories: MutableMap<Class<*>, AttValueFactory<Any>> = LinkedHashMap()
        val factoriesList = services.attValueFactories
        for (valueFactory in factoriesList) {
            for (type in valueFactory.getValueTypes()) {
                @Suppress("UNCHECKED_CAST")
                val factory = valueFactory as AttValueFactory<Any>
                valueFactories[type] = factory
                if (type.isInterface) {
                    factoriesByInterface[type] = factory
                }
            }
            factoriesByType[valueFactory::class.java] = valueFactory
        }
        for (valueFactory in factoriesList) {
            if (valueFactory is ServiceFactoryAware) {
                valueFactory.setRecordsServiceFactory(services)
            }
            valueFactory.init(this)
        }
        valueFactories
    }

    fun <T : AttValueFactory<*>> getFactory(type: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        val factory = factoriesByType[type] as? T
        return factory ?: error("Factory with type $type is not found")
    }

    fun toAttValue(value: Any?): AttValue? {

        if (value == null || value is EntityRef && EntityRef.isEmpty(value)) {
            return null
        }
        var valueToConvert = value

        if (valueToConvert is AttValue) {
            return valueToConvert
        }
        if (valueToConvert is AttValueCtx) {
            valueToConvert = valueToConvert.getValue()
            if (valueToConvert is AttValue) {
                return valueToConvert
            }
        }
        if (valueToConvert is MimeType) {
            valueToConvert = valueToConvert.toString()
        }
        val valueClazz = if (valueToConvert is EntityRef) {
            EntityRef::class.java
        } else {
            valueToConvert.javaClass
        }

        val factory: AttValueFactory<Any> = valueFactories[valueClazz]
            ?: findFactoryForInterfaces(valueClazz)
            ?: valueFactories[Any::class.java]
            ?: error("Factory can't be resolved for value: $valueToConvert")

        return factory.getValue(valueToConvert)
    }

    private fun findFactoryForInterfaces(clazz: Class<*>): AttValueFactory<Any>? {
        val interfaces = getAllInterfaces(clazz)
        for (iface in interfaces) {
            factoriesByInterface[iface]?.let { return it }
        }
        return null
    }

    private fun getAllInterfaces(clazz: Class<*>): Set<Class<*>> {
        val result = mutableSetOf<Class<*>>()
        collectInterfaces(clazz, result)
        return result
    }

    private fun collectInterfaces(clazz: Class<*>, result: MutableSet<Class<*>>) {
        for (iface in clazz.interfaces) {
            if (result.add(iface)) {
                collectInterfaces(iface, result)
            }
        }
        clazz.superclass?.let { collectInterfaces(it, result) }
    }
}
