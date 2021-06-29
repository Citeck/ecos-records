package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.atts.value.AttValue

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

            override fun getAs(type: String?): Any? {
                if (type == "mltext") {
                    return value.getAs(MLText::class.java)
                }
                return null
            }
        }
    }

    override fun getValueTypes() = listOf(ObjectData::class.java)
}
