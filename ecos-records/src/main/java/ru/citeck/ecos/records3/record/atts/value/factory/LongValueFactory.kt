package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records3.record.atts.value.AttValue

class LongValueFactory : AttValueFactory<Long> {

    override fun getValue(value: Long): AttValue {

        return object : AttValue {

            override fun asText(): String {
                return value.toString()
            }

            override fun asDouble(): Double {
                return value.toDouble()
            }

            override fun asBoolean(): Boolean {
                return value != 0L
            }

            override fun asRaw(): Any {
                return value
            }
        }
    }

    override fun getValueTypes() = listOf(java.lang.Long::class.java)
}
