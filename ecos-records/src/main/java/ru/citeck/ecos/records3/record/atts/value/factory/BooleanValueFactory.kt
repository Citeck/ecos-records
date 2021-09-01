package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.records3.record.atts.value.AttValue

class BooleanValueFactory : AttValueFactory<Boolean> {

    override fun getValue(value: Boolean): AttValue? {

        return object : AttValue {

            override fun asText(): String {
                return value.toString()
            }

            override fun asBoolean(): Boolean {
                return value
            }

            override fun asRaw(): Any {
                return value
            }
        }
    }

    override fun getValueTypes() = listOf(java.lang.Boolean::class.java)
}
