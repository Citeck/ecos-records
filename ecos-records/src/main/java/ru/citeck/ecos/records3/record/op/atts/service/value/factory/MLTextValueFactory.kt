package ru.citeck.ecos.records3.record.op.atts.service.value.factory

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.*

class MLTextValueFactory : AttValueFactory<MLText> {

    override fun getValue(value: MLText): AttValue? {
        return Value(value, false)
    }

    internal class Value(val value: MLText, private val closest: Boolean) : AttValue {

        override fun asText(): String? {
            return value.getClosestValue(RequestContext.getLocale())
        }

        override fun getAtt(name: String): Any? {
            if (name == "closest") {
                return Value(value, true)
            }
            if (name == "exact") {
                return Value(value, false)
            }
            val locale = Locale(name)
            return if (closest) {
                value.getClosestValue(locale)
            } else {
                value.get(locale)
            }
        }

        override fun asJson(): Any? {
            return value.getValues()
        }

        override fun has(name: String): Boolean {
            return value.has(Locale(name))
        }
    }

    override fun getValueTypes() = listOf(MLText::class.java)
}
