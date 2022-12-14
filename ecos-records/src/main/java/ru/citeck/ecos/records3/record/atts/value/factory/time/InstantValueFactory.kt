package ru.citeck.ecos.records3.record.atts.value.factory.time

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.factory.AttValueFactory
import java.time.Instant

class InstantValueFactory : AttValueFactory<Instant> {

    override fun getValue(value: Instant): AttValue {
        return InstantValue(value)
    }

    override fun getValueTypes() = listOf(Instant::class.java)

    class InstantValue(private val instant: Instant) : AttValue {

        override fun asText(): String {
            return instant.toString()
        }

        override fun asDouble(): Double {
            return instant.toEpochMilli().toDouble()
        }
    }
}
