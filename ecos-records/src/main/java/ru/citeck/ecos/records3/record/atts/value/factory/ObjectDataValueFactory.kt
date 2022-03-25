package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter

class ObjectDataValueFactory : AttValueFactory<ObjectData> {

    private lateinit var dataValueFactory: DataValueAttFactory

    override fun init(attValuesConverter: AttValuesConverter) {
        this.dataValueFactory = attValuesConverter.getFactory(DataValueAttFactory::class.java)
    }

    override fun getValue(value: ObjectData): AttValue? {
        return dataValueFactory.getValue(value.getData())
    }

    override fun getValueTypes() = listOf(ObjectData::class.java)
}
