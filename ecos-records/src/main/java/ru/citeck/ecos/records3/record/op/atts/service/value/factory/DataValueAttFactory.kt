package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue

class DataValueAttFactory : AttValueFactory<DataValue> {

    override fun getValue(value: DataValue): AttValue? {

        return object : AttValue {

            override fun asText(): String? {
                return if (value.isValueNode()) {
                    value.asText()
                } else {
                    Json.mapper.toString(value)
                }
            }

            override fun getAtt(name: String): Any? {
                return value.get(name)
            }

            override fun asBoolean(): Boolean? {
                return value.asBoolean()
            }
        }
    }

    override fun getValueTypes() = listOf(DataValue::class.java)
}
