package ru.citeck.ecos.records3.record.atts.proc

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import java.util.*

class AttHexProcessor : AbstractAttProcessor<AttHexProcessor.Arguments>(false) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun processOne(attributes: ObjectData, value: DataValue, args: Arguments): Any? {

        if (!value.isTextual()) {
            return null
        }

        val bytesBase64 = value.asText()
        if (bytesBase64.isBlank()) {
            return null
        }

        val bytes = try {
            Base64.getDecoder().decode(bytesBase64)
        } catch (e: Exception) {
            log.error { "Incorrect base64 string: $bytesBase64" }
            return null
        }

        return bytes.joinToString(args.delimiter) {
            (it.toInt() and 0xFF).toString(16)
                .toUpperCase()
                .padStart(2, '0')
        }
    }

    override fun parseArgs(args: List<DataValue>): Arguments {
        if (args.isEmpty()) {
            return Arguments()
        }
        return Arguments(args[0].asText())
    }

    override fun getType(): String = "hex"

    class Arguments(
        val delimiter: String = ""
    )
}
