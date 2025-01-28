package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter

/**
 * AttValueFactory creates adapter between any value
 * from data source and universal interface AttValue
 *
 * You can register custom AttValue factory by placing
 * file resources/META-INF/services/ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
 * with defining your custom implementation
 */
interface AttValueFactory<T> {

    fun init(attValuesConverter: AttValuesConverter) {}

    fun getValue(value: T): AttValue?

    fun getValueTypes(): List<Class<*>>

    fun getPriority(): Int = 0
}
