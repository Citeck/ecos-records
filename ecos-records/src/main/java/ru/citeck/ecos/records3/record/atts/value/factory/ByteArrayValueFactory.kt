package ru.citeck.ecos.records3.record.atts.value.factory

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.atts.value.AttValue
import java.nio.charset.StandardCharsets
import java.util.*

class ByteArrayValueFactory : AttValueFactory<ByteArray> {

    override fun getValue(value: ByteArray): AttValue {
        return object : AttValue {

            override fun asText(): String? {
                return Base64.getEncoder().encodeToString(value)
            }

            override fun getAs(type: String): Any? {
                return if ("string" == type) {
                    String(value, StandardCharsets.UTF_8)
                } else null
            }

            override fun asJson(): Any? {
                return Json.mapper.read(value, JsonNode::class.java)
            }
        }
    }

    override fun getValueTypes() = listOf(ByteArray::class.java)
}
