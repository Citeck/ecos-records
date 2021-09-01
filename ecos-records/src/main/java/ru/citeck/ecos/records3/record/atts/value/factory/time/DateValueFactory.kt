package ru.citeck.ecos.records3.record.atts.value.factory.time

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
import java.time.Instant
import java.util.*

class DateValueFactory : AttValueFactory<Date> {

    override fun getValue(value: Date): AttValue {
        return DateValue(value)
    }

    override fun getValueTypes() = listOf(Date::class.java)

    class DateValue(private val date: Date) : AttValue {

        override fun asText(): String {
            return Instant.ofEpochMilli(date.time).toString()
        }

        override fun asDouble(): Double {
            return date.time.toDouble()
        }
    }
}
