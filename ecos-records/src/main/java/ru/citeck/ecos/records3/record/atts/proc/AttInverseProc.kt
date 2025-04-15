package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

class AttInverseProc : AbstractAttProcessor<Unit>(true) {

    override fun processOne(attributes: ObjectData, value: DataValue, args: Unit): Any {
        return value.asBoolean().not()
    }

    override fun parseArgs(args: List<DataValue>) {}

    override fun getType(): String {
        return "inv"
    }
}
