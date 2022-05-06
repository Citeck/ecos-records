package ru.citeck.ecos.records3.record.atts.value.factory

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.records3.record.atts.value.AttValue
import java.util.*

class MLTextValueFactory : AttValueFactory<MLText> {

    override fun getValue(value: MLText): AttValue {
        return Value(value, false)
    }

    internal class Value(val value: MLText, private val closest: Boolean) : AttValue {

        override fun asText(): String {
            return value.getClosestValue(I18nContext.getLocale())
        }

        override fun getAtt(name: String): Any {
            if (name == "closest") {
                return Value(value, true)
            }
            if (name == "exact") {
                return Value(value, false)
            }
            val locale = if (name.contains("_")) {
                val countryAndLang = name.split("_")
                Locale(countryAndLang[0], countryAndLang[1])
            } else {
                Locale(name)
            }
            return if (closest) {
                value.getClosestValue(locale)
            } else {
                value.get(locale)
            }
        }

        override fun asJson(): Any {
            return value.getValues()
        }

        override fun has(name: String): Boolean {
            return value.has(Locale(name))
        }

        override fun getAs(type: String): Any? {
            if (type == "mltext") {
                return this
            }
            return null
        }

        override fun asRaw(): Any {
            return value
        }
    }

    override fun getValueTypes() = listOf(MLText::class.java)
}
