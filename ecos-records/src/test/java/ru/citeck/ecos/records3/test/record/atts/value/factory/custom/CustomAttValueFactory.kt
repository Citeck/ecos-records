package ru.citeck.ecos.records3.test.record.atts.value.factory.custom

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
import ru.citeck.ecos.records3.record.atts.value.factory.DataValueAttFactory

class CustomAttValueFactory : AttValueFactory<CustomDto> {

    private lateinit var dataValueAttFactory: DataValueAttFactory

    override fun init(attValuesConverter: AttValuesConverter) {
        dataValueAttFactory = attValuesConverter.getFactory(DataValueAttFactory::class.java)
    }

    override fun getValue(value: CustomDto): AttValue? {
        return dataValueAttFactory.getValue(value.value)
    }

    override fun getValueTypes(): List<Class<*>> = listOf(CustomDto::class.java)
}
