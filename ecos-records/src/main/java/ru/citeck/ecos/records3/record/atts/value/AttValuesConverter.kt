package ru.citeck.ecos.records3.record.atts.value

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
import ru.citeck.ecos.records3.record.atts.value.impl.meta.AttMetaValue

class AttValuesConverter(private val services: RecordsServiceFactory) {

    private val valueFactories: Map<Class<*>, AttValueFactory<Any>> by lazy {
        val valueFactories: MutableMap<Class<*>, AttValueFactory<Any>> = LinkedHashMap()
        for (valueFactory in services.attValueFactories) {
            for (type in valueFactory.getValueTypes()) {
                @Suppress("UNCHECKED_CAST")
                valueFactories[type] = valueFactory as AttValueFactory<Any>
            }
        }
        valueFactories
    }

    fun toAttValue(value: Any?): AttValue? {

        if (value == null || value is RecordRef && RecordRef.isEmpty(value)) {
            return null
        }
        if (value is AttValue) {
            return value
        }
        if (value is MetaValue) {
            return AttMetaValue(value)
        }
        val factory: AttValueFactory<Any> = valueFactories[value.javaClass]
            ?: (valueFactories[Any::class.java] ?: error("Factory can't be resolved for value: $value"))

        return factory.getValue(value)
    }
}
