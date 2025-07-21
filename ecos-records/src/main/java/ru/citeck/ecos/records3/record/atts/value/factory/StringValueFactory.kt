package ru.citeck.ecos.records3.record.atts.value.factory

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.lang.NumberFormatException
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class StringValueFactory : AttValueFactory<String> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val converters = ConcurrentHashMap<String, (String) -> Any?>()

    init {
        converters["ref"] = { EntityRef.valueOf(it) }
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
                if (value.isEmpty()) {
                    return null
                }
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
            log.warn { "Converter with type $type ($currentConverter) will be replaced by $converter" }
        }
        converters[type] = converter
    }

    override fun getValueTypes() = listOf(String::class.java)
}
