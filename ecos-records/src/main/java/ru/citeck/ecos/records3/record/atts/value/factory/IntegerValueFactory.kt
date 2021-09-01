package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records3.record.atts.value.AttValue

class IntegerValueFactory : AttValueFactory<Int> {

    override fun getValue(value: Int): AttValue {

        return object : AttValue {

            override fun asText(): String {
                return value.toString()
            }

            override fun asDouble(): Double {
                return java.lang.Double.valueOf(value.toDouble())
            }

            override fun asBoolean(): Boolean {
                return value != 0
            }

            override fun asRaw(): Any {
                return value
            }
        }
    }

    override fun getValueTypes() = listOf(Integer::class.java)
}
