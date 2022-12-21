package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter
import ru.citeck.ecos.records3.record.atts.value.HasListView

class DataValueAttFactory : AttValueFactory<DataValue> {

    companion object {
        fun getAsText(value: DataValue): String? {
            return if (value.isNull()) {
                null
            } else if (value.isValueNode()) {
                value.asText()
            } else {
                value.toString()
            }
        }
    }

    private lateinit var valuesConverter: AttValuesConverter

    override fun init(attValuesConverter: AttValuesConverter) {
        this.valuesConverter = attValuesConverter
    }

    override fun getValue(value: DataValue): AttValue? {
        if (value.isObject()) {
            return DataValueAttValue(value)
        }
        val unboxed = unboxAttValue(value)
        if (unboxed !is DataValue) {
            return valuesConverter.toAttValue(unboxed)
        }
        return DataValueAttValue(unboxed)
    }

    private fun unboxAttValue(value: DataValue): Any? {
        return when {
            value.isNull() -> null
            value.isTextual() -> value.textValue()
            value.isBoolean() -> value.booleanValue()
            value.isInt() -> value.intValue()
            value.isLong() -> value.longValue()
            value.isFloat() -> value.floatValue()
            value.isDouble() -> value.doubleValue()
            value.isBinary() -> value.binaryValue()
            value.isArray() -> {
                val list = ArrayList<Any?>(value.size())
                for (inner in value) {
                    list.add(unboxAttValue(inner))
                }
                list
            }
            else -> value
        }
    }

    override fun getValueTypes() = listOf(DataValue::class.java)

    private inner class DataValueAttValue(val value: DataValue) : AttValue, HasListView<DataValue> {

        override fun getId(): Any? {
            if (value.isTextual()) {
                return value.asText()
            } else if (value.isObject() && value.has("id")) {
                return value["id"].asText().ifEmpty { null }
            }
            return null
        }

        override fun asText(): String? {
            return getAsText(value)
        }

        override fun getAtt(name: String): Any? {
            val res = value[name]
            return if (res.isObject()) {
                getValue(res)
            } else {
                unboxAttValue(res)
            }
        }

        override fun asBoolean(): Boolean? {
            if (value.isBoolean()) {
                return value.asBoolean()
            } else if (value.isTextual()) {
                return value.asText() == true.toString()
            }
            return null
        }

        override fun getListView(): List<DataValue> {
            if (value.isArray()) {
                return value.toList()
            }
            return listOf(value)
        }

        override fun asDouble(): Double? {
            if (value.isNumber() || value.isTextual()) {
                return value.asDouble()
            }
            return null
        }

        override fun asBin(): Any {
            return value
        }

        override fun has(name: String): Boolean {
            return value.has(name)
        }

        override fun getAs(type: String): Any? {
            if (type == "mltext") {
                return value.getAs(MLText::class.java)
            }
            return null
        }

        override fun asRaw(): Any {
            return value
        }

        override fun asJson(): Any {
            return value
        }

        override fun toString(): String {
            return value.toString()
        }
    }
}
