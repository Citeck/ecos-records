package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class LongValueFactory : AttValueFactory<Long> {

    override fun getValue(value: Long): AttValue? {

        return object : AttValue {

            override fun asText(): String? {
                return value.toString()
            }

            override fun asDouble(): Double? {
                return value.toDouble()
            }

            override fun asBoolean(): Boolean? {
                return value != 0L
            }
        }
    }

    override fun getValueTypes() = listOf(Long::class.java)
}
