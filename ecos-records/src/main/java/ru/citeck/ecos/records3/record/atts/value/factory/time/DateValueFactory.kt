package ru.citeck.ecos.records3.record.atts.value.factory.time

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
import java.time.Instant
import java.util.*

class DateValueFactory : AttValueFactory<Date> {

    private lateinit var instantValueFactory: InstantValueFactory

    override fun init(attValuesConverter: AttValuesConverter) {
        this.instantValueFactory = attValuesConverter.getFactory(InstantValueFactory::class.java)
    }

    override fun getValue(value: Date): AttValue {
        return instantValueFactory.getValue(Instant.ofEpochMilli(value.time))
    }

    override fun getValueTypes() = listOf(Date::class.java)
}
