package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ecos.com.fasterxml.jackson210.databind.util.ISO8601Utils
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import java.util.*

class DateValueFactory : AttValueFactory<Date> {

    override fun getValue(value: Date): AttValue? {
        return DateValue(value)
    }

    override fun getValueTypes() = listOf(Date::class.java)

    class DateValue(private val date: Date) : AttValue {
        override fun asText(): String {
            return ISO8601Utils.format(date)
        }
    }
}
