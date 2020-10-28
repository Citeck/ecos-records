package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue


class ObjectDataValueFactory : AttValueFactory<ObjectData> {
    override fun getValue(value: ObjectData): AttValue? {
        return object : AttValue {
            override fun asText(): String? {
                return mapper.toString(value)
            }

            override fun getAtt(name: String): Any? {
                return value.get(name)
            }

            override fun asJson(): Any? {
                return value
            }
        }
    }

    override val valueTypes: MutableList<Class<out T?>?>?
        get() = listOf(ObjectData::class.java)
}
