package ru.citeck.ecos.records3.record.op.atts.service.value.factory.time

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.op.atts.service.value.factory.AttValueFactory
import java.time.Instant

class InstantValueFactory : AttValueFactory<Instant> {

    override fun getValue(value: Instant): AttValue? {
        return DateValue(value)
    }

    override fun getValueTypes() = listOf(Instant::class.java)

    class DateValue(private val date: Instant) : AttValue {

        override fun asText(): String {
            return date.toString()
        }

        override fun asDouble(): Double? {
            return date.toEpochMilli().toDouble()
        }
    }
}
