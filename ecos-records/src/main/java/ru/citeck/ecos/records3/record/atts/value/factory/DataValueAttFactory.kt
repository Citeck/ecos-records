package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.HasListView

class DataValueAttFactory : AttValueFactory<DataValue> {

    override fun getValue(value: DataValue): AttValue? {

        return object : AttValue, HasListView<DataValue> {

            override fun getId(): Any? {
                if (value.isTextual()) {
                    return value.asText()
                }
                return null
            }

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

            override fun getAs(type: String?): Any? {
                if (type == "mltext") {
                    return value.getAs(MLText::class.java)
                }
                return null
            }
        }
    }

    override fun getValueTypes() = listOf(DataValue::class.java)
}
