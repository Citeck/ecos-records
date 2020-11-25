package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.op.atts.service.value.HasListView

class DataValueAttFactory : AttValueFactory<DataValue> {

    override fun getValue(value: DataValue): AttValue? {

        return object : AttValue, HasListView<DataValue> {

            override fun asText(): String? {
                return if (value.isValueNode()) {
                    value.asText()
                } else {
                    Json.mapper.toString(value)
                }
            }

            override fun getAtt(name: String): Any? {
                val res = value.get(name)
                return if (res.isNull()) {
                    null
                } else {
                    getValue(res)
                }
            }

            override fun asBoolean(): Boolean? {
                return value.asBoolean()
            }

            override fun getListView(): List<DataValue> {
                if (value.isArray()) {
                    return value.toList()
                }
                return listOf(value)
            }
        }
    }

    override fun getValueTypes() = listOf(DataValue::class.java)
}
