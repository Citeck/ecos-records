package ru.citeck.ecos.records3.record.atts.value.factory

import mu.KotlinLogging
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.value.AttValue
import java.lang.NumberFormatException
import java.util.concurrent.ConcurrentHashMap

class StringValueFactory : AttValueFactory<String> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val converters = ConcurrentHashMap<String, (String) -> Any?>()

    init {
        converters["ref"] = { RecordRef.valueOf(it) }
        converters["mltext"] = { MLText(it) }
    }

    override fun getValue(value: String): AttValue {

        return object : AttValue {

            override fun getId(): Any {
                return value
            }

            override fun asText(): String {
                return value
            }

            override fun asDouble(): Double? {
                return try {
                    value.toDouble()
                } catch (e: NumberFormatException) {
                    null
                }
            }

            override fun getAs(type: String): Any? {
                return converters[type]?.invoke(value)
            }

            override fun asRaw(): Any {
                return value
            }

            override fun asBin(): Any {
                return value
            }
        }
    }

    fun addConverter(type: String, converter: (String) -> Any?) {
        val currentConverter = converters[type]
        if (currentConverter != null) {
            log.warn("Converter with type $type ($currentConverter) will be replaced by $converter")
        }
        converters[type] = converter
    }

    override fun getValueTypes() = listOf(String::class.java)
}
