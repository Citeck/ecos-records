package ru.citeck.ecos.records3.record.op.atts.service.value

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.SimpleAttEdge

/**
 * Attribute value. Used to get attributes by schema.
 *
 * @author Pavel Simonov
 */
interface AttValue {

    fun getId(): Any? = null

    @Throws(Exception::class)
    fun getDisplayName(): String? = asText()

    @Throws(Exception::class)
    fun asText() : String? = toString()

    @Throws(Exception::class)
    fun getAs(type: String) : Any? = null

    @Throws(Exception::class)
    fun asDouble(): Double? = asText()?.toDouble()

    @Throws(Exception::class)
    fun asBoolean(): Boolean? {
        return when (asText()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    @Throws(Exception::class)
    fun asJson(): Any? = Json.mapper.read(asText())

    @Throws(Exception::class)
    fun has(name: String): Boolean = false

    @Throws(Exception::class)
    fun getAtt(name: String): Any? = null

    @Throws(Exception::class)
    fun getEdge(name: String): AttEdge? = SimpleAttEdge(name, this)

    @Throws(Exception::class)
    fun getType() : RecordRef = RecordRef.EMPTY
}
