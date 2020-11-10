package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

interface AttValueFactory<T> {

    fun getValue(value: T): AttValue?

    fun getValueTypes(): List<Class<*>>
}
