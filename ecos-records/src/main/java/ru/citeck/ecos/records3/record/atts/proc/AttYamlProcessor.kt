package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.YamlUtils

class AttYamlProcessor : AbstractAttProcessor<List<DataValue>>() {

    override fun processOne(attributes: ObjectData, value: DataValue, args: List<DataValue>): Any? {
        if (!value.isObject()) {
            return null
        }
        return YamlUtils.toString(value)
    }

    override fun parseArgs(args: List<DataValue>): List<DataValue> {
        return args
    }

    override fun getType(): String = "yaml"
}
