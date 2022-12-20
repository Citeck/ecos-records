package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter

interface AttValueFactory<T> {

    fun init(attValuesConverter: AttValuesConverter) {}

    fun getValue(value: T): AttValue?

    fun getValueTypes(): List<Class<*>>

    fun getPriority(): Int = 0
}
