package ru.citeck.ecos.records3.record.atts.value.factory.time

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
import java.time.OffsetDateTime

class OffsetDateTimeValueFactory : AttValueFactory<OffsetDateTime> {

    override fun getValue(value: OffsetDateTime): AttValue {
        return DateValue(value)
    }

    override fun getValueTypes() = listOf(OffsetDateTime::class.java)

    class DateValue(private val date: OffsetDateTime) : AttValue {

        override fun asText(): String {
            return date.toInstant().toString()
        }

        override fun asDouble(): Double {
            return date.toInstant().toEpochMilli().toDouble()
        }
    }
}
