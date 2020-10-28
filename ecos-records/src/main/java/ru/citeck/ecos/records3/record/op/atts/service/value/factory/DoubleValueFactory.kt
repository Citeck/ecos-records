package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class DoubleValueFactory : AttValueFactory<Double> {

    override fun getValue(value: Double): AttValue? {
        return object : AttValue {
            override fun asText(): String? {
                return value.toString()
            }

            override fun asDouble(): Double? {
                return value
            }

            override fun asBoolean(): Boolean? {
                return value != 0.0
            }
        }
    }

    override fun getValueTypes() = listOf(Double::class.java)
}
