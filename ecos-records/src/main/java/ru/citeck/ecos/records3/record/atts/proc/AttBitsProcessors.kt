package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData

object AttBitsProcessors {

    val processors: List<AttProcessor> = run {
        val processors = ArrayList<AttProcessor>()
        processors.add(AttBitIsSetProcessor())
        AttBitAndOrProcessor.Operation.entries.forEach {
            processors.add(AttBitAndOrProcessor(it))
        }
        AttBitShiftProcessor.Operation.entries.forEach {
            processors.add(AttBitShiftProcessor(it))
        }
        processors
    }

    private fun parseLongValue(value: DataValue): Long {
        return if (value.isNumber()) {
            value.asLong()
        } else if (value.isTextual()) {
            value.asText().toLongOrNull() ?: 0L
        } else {
            0L
        }
    }

    class AttBitIsSetProcessor : AbstractAttProcessor<AttBitIsSetProcessor.Args>(true) {

        companion object {
            const val TYPE = "bitIsSet"
        }

        override fun getType(): String {
            return TYPE
        }

        override fun processOne(attributes: ObjectData, value: DataValue, args: Args): Any {
            val num = parseLongValue(value)
            if (args.mask < 0 || num == 0L) {
                return false
            }
            return num.and(args.mask) != 0L
        }

        override fun parseArgs(args: List<DataValue>): Args {
            if (args.isEmpty() || !args[0].isNumber()) {
                return Args(-1)
            }
            val bitIdx = args[0].asInt()
            if (bitIdx >= 64) {
                return Args(-1)
            }
            return Args(1L shl bitIdx)
        }

        data class Args(
            val mask: Long
        )
    }

    class AttBitAndOrProcessor(
        private val operation: Operation
    ) : AbstractAttProcessor<AttBitAndOrProcessor.Args>(false) {

        override fun processOne(attributes: ObjectData, value: DataValue, args: Args): Long {
            return operation.apply(parseLongValue(value), args.value)
        }

        override fun parseArgs(args: List<DataValue>): Args {
            if (args.isEmpty() || !args[0].isNumber()) {
                return Args(0)
            }
            return Args(args[0].asLong())
        }

        override fun getType(): String {
            return operation.procType
        }

        data class Args(
            val value: Long
        )

        enum class Operation(
            val procType: String,
            val apply: (Long, Long) -> Long
        ) {
            AND("bitAnd", { num, value -> num and value }),
            OR("bitOr", { num, value -> num or value })
        }
    }

    class AttBitShiftProcessor(
        private val operation: Operation
    ) : AbstractAttProcessor<AttBitShiftProcessor.Args>(false) {

        override fun getType(): String {
            return operation.procType
        }

        override fun processOne(attributes: ObjectData, value: DataValue, args: Args): Any? {
            if (args.bitsCount < 0) {
                return null
            }
            val numValue = parseLongValue(value)
            if (args.bitsCount == 0 || numValue == 0L) {
                return numValue
            }
            return operation.apply(value.asLong(), args.bitsCount)
        }

        override fun parseArgs(args: List<DataValue>): Args {
            if (args.isEmpty() || !args[0].isNumber()) {
                return Args(-1)
            }
            val bitIdx = args[0].asInt()
            if (bitIdx >= 64) {
                return Args(0)
            }
            return Args(bitIdx)
        }

        data class Args(
            val bitsCount: Int
        )

        enum class Operation(
            val procType: String,
            val apply: (Long, Int) -> Long
        ) {
            SHL("bitShl", { num, bits -> num shl bits }),
            SHR("bitShr", { num, bits -> num shr bits }),
            USHR("bitUShr", { num, bits -> num ushr bits })
        }
    }
}
