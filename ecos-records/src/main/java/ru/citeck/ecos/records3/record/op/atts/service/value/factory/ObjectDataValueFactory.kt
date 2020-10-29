package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class ObjectDataValueFactory : AttValueFactory<ObjectData> {

    override fun getValue(value: ObjectData): AttValue? {

        return object : AttValue {

            override fun asText(): String? {
                return Json.mapper.toString(value)
            }

            override fun getAtt(name: String): Any? {
                return value.get(name)
            }

            override fun asJson(): Any? {
                return value
            }
        }
    }

    override fun getValueTypes() = listOf(ObjectData::class.java)
}
