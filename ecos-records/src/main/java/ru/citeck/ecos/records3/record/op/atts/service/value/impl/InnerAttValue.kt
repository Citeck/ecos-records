package ru.citeck.ecos.records3.record.op.atts.service.value.impl

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode
import ecos.com.fasterxml.jackson210.databind.node.MissingNode
import ecos.com.fasterxml.jackson210.databind.node.NullNode
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue
import java.util.*
import java.util.function.Function


class InnerAttValue : AttValue, HasCollectionView<InnerAttValue?> {
    private val value: JsonNode?

    constructor(value: Any?) {
        this.value = mapper.convert(value, JsonNode::class.java)
    }

    constructor(value: JsonNode?) {
        if (value == null || value is MissingNode) {
            this.value = NullNode.getInstance()
        } else {
            this.value = value
        }
    }

    override fun getAtt(name: String): Any? {
        val node = value!!.path(name)
        return if (node!!.isMissingNode || node.isNull) {
            null
        } else InnerAttValue(node)
    }

    override fun getAs(type: String): Any? {
        val node: JsonNode = value.path(RecordConstants.ATT_AS).path(type)
        return if (node.isMissingNode || node.isNull) {
            null
        } else InnerAttValue(node)
    }

    val collectionView: MutableCollection<InnerAttValue?>?
        get() {
            if (value is ArrayNode) {
                val result: MutableList<InnerAttValue?> = ArrayList()
                for (`val` in value) {
                    result.add(InnerAttValue(`val`))
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
        } else node.asBoolean()
    }

    override fun getDisplayName(): String? {
        return getScalar<String?>(value, "?disp", Function { obj: JsonNode? -> obj!!.asText() })
    }

    override fun asText(): String? {
        return getScalar<String?>(value, "?str", Function { obj: JsonNode? -> obj!!.asText() })
    }

    override fun getId(): String? {
        return getScalar<String?>(value, "?id", Function { obj: JsonNode? -> obj!!.asText() })
    }

    override fun asDouble(): Double? {
        return getScalar<Double?>(value, "?num", Function { obj: JsonNode? -> obj!!.asDouble() })
    }

    override fun asBoolean(): Boolean? {
        return getScalar<Boolean?>(value, "?bool", Function { obj: JsonNode? -> obj!!.asBoolean() })
    }

    override fun asJson(): Any? {
        return getScalar<JsonNode?>(value, "?json", Function { n: JsonNode? -> n })
    }

    companion object {
        private fun <T> getScalar(node: JsonNode?, name: String?, mapper: Function<JsonNode?, T?>?): T? {
            val scalar = node!!.path(name)
            return if (scalar!!.isNull || scalar.isMissingNode) {
                null
            } else mapper!!.apply(scalar)
        }
    }
}
