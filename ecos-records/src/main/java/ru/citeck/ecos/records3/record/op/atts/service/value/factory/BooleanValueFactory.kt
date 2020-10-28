package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class BooleanValueFactory : AttValueFactory<Boolean> {

    override fun getValue(value: Boolean): AttValue? {

        return object : AttValue {

            override fun asText(): String? {
                return value.toString()
            }

            override fun asBoolean(): Boolean? {
                return value
            }
        }
    }

    override fun getValueTypes() = listOf(Boolean::class.java)
}
