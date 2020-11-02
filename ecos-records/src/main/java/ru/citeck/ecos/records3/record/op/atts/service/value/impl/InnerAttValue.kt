package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import ru.citeck.ecos.records3.record.op.atts.service.value.HasCollectionView
import java.util.*

class InnerAttValue(value: Any?) : AttValue, HasCollectionView<InnerAttValue> {

    companion object {

        private fun <T> getScalar(node: JsonNode, name: String, mapper: (JsonNode) -> T): T? {
            val scalar = node.path(name)
            return if (scalar.isNull || scalar.isMissingNode) {
                null
            } else {
                mapper.invoke(scalar)
            }
        }
    }

    private val value: JsonNode = Json.mapper.toJson(value)

    override fun getAtt(name: String): Any? {
        val node = value.path(name)
        return if (node.isMissingNode || node.isNull) {
            null
        } else {
            InnerAttValue(node)
        }
    }

    override fun getAs(type: String): Any? {
        val node: JsonNode = value.path(RecordConstants.ATT_AS).path(type)
        return if (node.isMissingNode || node.isNull) {
            null
        } else {
            InnerAttValue(node)
        }
    }

    override fun getCollectionView(): Collection<InnerAttValue> {
        if (value is ArrayNode) {
            val result: MutableList<InnerAttValue> = ArrayList()
            for (elem in value) {
                result.add(InnerAttValue(elem))
            }
            return result
        }
        return listOf(this)
    }

    override fun has(name: String): Boolean {
        val node: JsonNode = value.path(RecordConstants.ATT_HAS)
            .path(name)
            .path("?bool")
        return if (node.isMissingNode || node.isNull) {
            false
        } else {
            node.asBoolean()
        }
    }

    override fun getDisplayName(): String? {
        return getScalar(value, "?disp") { it.asText() }
    }

    override fun asText(): String? {
        return getScalar(value, "?str") { it.asText() }
    }

    override fun getId(): String? {
        return getScalar(value, "?id") { it.asText() }
    }

    override fun asDouble(): Double? {
        return getScalar(value, "?num") { it.asDouble() }
    }

    override fun asBoolean(): Boolean? {
        return getScalar(value, "?bool") { it.asBoolean() }
    }

    override fun asJson(): Any? {
        return getScalar(value, "?json") { it }
    }
}
