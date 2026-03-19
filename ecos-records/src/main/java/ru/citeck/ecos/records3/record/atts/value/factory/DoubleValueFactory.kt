package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records3.record.atts.value.AttValue
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class DoubleValueFactory : AttValueFactory<Double> {

    override fun getValue(value: Double): AttValue {

        return object : AttValue {

            override fun asText(): String? {
                val format = DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
                format.maximumFractionDigits = 340
                return format.format(value)
            }

            override fun asDouble(): Double {
                return value
            }

            override fun asBoolean(): Boolean {
                return value != 0.0
            }

            override fun asRaw(): Any {
                return value
            }
        }
    }

    override fun getValueTypes() = listOf(Double::class.java, java.lang.Double::class.java)
}
