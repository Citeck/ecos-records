package ru.citeck.ecos.records3.record.atts.value

import ru.citeck.ecos.records2.ServiceFactoryAware
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
import ru.citeck.ecos.records3.record.atts.value.impl.meta.AttMetaValue
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.mime.MimeType

class AttValuesConverter(private val services: RecordsServiceFactory) {

    private val factoriesByType = HashMap<Class<out AttValueFactory<*>>, AttValueFactory<*>>()

    private val valueFactories: Map<Class<*>, AttValueFactory<Any>> by lazy {
        val valueFactories: MutableMap<Class<*>, AttValueFactory<Any>> = LinkedHashMap()
        val factoriesList = services.attValueFactories
        for (valueFactory in factoriesList) {
            for (type in valueFactory.getValueTypes()) {
                @Suppress("UNCHECKED_CAST")
                valueFactories[type] = valueFactory as AttValueFactory<Any>
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
        if (valueToConvert is MetaValue) {
            return AttMetaValue(valueToConvert)
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
            ?: (valueFactories[Any::class.java] ?: error("Factory can't be resolved for value: $valueToConvert"))

        return factory.getValue(valueToConvert)
    }
}
