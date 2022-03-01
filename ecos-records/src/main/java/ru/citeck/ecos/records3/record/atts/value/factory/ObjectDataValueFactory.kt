package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.record.atts.value.AttValue

class ObjectDataValueFactory(
    private val dataValueFactory: DataValueAttFactory
) : AttValueFactory<ObjectData> {

    override fun getValue(value: ObjectData): AttValue {
        return dataValueFactory.getValue(value.getData())
    }

    override fun getValueTypes() = listOf(ObjectData::class.java)
}
