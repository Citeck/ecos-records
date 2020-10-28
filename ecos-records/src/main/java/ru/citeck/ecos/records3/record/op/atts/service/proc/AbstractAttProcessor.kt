package ru.citeck.ecos.records3.record.op.atts.service.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

abstract class AbstractAttProcessor<T> : AttProcessor {

    private var allowNull = false

    constructor()

    constructor(allowNull: Boolean) {
        this.allowNull = allowNull
    }

    override fun process(attributes: ObjectData, value: DataValue, args: List<DataValue>): Any? {
        if (!allowNull && value.isNull()) {
            return value
        }
        val arguments = parseArgs(args)
        return if (value.isArray()) {
            val res: DataValue = DataValue.createArr()
            value.forEach { res.add(processOne(attributes, it, arguments)) }
            res
        } else {
            processOne(attributes, value, arguments)
        }
    }

    protected abstract fun processOne(attributes: ObjectData, value: DataValue, args: T): Any?

    protected abstract fun parseArgs(args: List<DataValue>): T
}
