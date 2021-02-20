package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records3.record.atts.value.AttValue

interface AttValueFactory<T> {

    fun getValue(value: T): AttValue?

    fun getValueTypes(): List<Class<*>>
}
