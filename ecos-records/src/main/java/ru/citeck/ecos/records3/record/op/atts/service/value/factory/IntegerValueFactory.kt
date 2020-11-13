package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class IntegerValueFactory : AttValueFactory<Int> {

    override fun getValue(value: Int): AttValue? {

        return object : AttValue {

            override fun asText(): String? {
                return value.toString()
            }

            override fun asDouble(): Double? {
                return java.lang.Double.valueOf(value.toDouble())
            }

            override fun asBoolean(): Boolean? {
                return value != 0
            }
        }
    }

    override fun getValueTypes() = listOf(Integer::class.java)
}
