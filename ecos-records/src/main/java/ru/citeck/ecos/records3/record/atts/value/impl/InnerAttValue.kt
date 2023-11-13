package ru.citeck.ecos.records3.record.atts.value.impl

import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode
import ecos.com.fasterxml.jackson210.databind.node.TextNode
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.atts.value.AttValueProxy
import ru.citeck.ecos.records3.record.atts.value.HasListView
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

class InnerAttValue(value: Any?) : AttValue, HasListView<InnerAttValue>, AttValueProxy {

    companion object {

        private fun <T> getScalar(node: JsonNode, name: String, mapper: (JsonNode) -> T): T? {
            val scalar = node.path(name)
            return if (scalar.isNull || scalar.isMissingNode) {
                val type = ScalarType.getBySchema(name)
                if (type != null && node.has(type.mirrorAtt)) {
                    getScalar(node.path(type.mirrorAtt), name, mapper)
                } else {
                    null
                }
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
            if (node is TextNode) {
                node.asText()
            } else {
                InnerAttValue(node)
            }
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

    override fun getListView(): List<InnerAttValue> {
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
        var node: JsonNode = value.path(RecordConstants.ATT_HAS)
        if (node.isBoolean) {
            return node.asBoolean()
        }
        node = node.path(name)
        if (node.isBoolean) {
            return node.asBoolean()
        }
        node = node.path(ScalarType.BOOL.schema)
        return if (node.isMissingNode || node.isNull) {
            false
        } else {
            node.asBoolean()
        }
    }

    override fun getDisplayName(): Any? {
        return getScalar(value, ScalarType.DISP.schema) { it.asText() }
    }

    override fun asText(): String? {
        return getScalar(value, ScalarType.STR.schema) { it.asText() }
    }

    override fun getId(): Any? {
        val id = getScalar(value, ScalarType.ID.schema) { Json.mapper.toJava(it) }
        return id ?: getScalar(value, ScalarType.ASSOC.schema) { Json.mapper.toJava(it) }
    }

    override fun asDouble(): Double? {
        return getScalar(value, ScalarType.NUM.schema) { it.asDouble() }
    }

    override fun asBoolean(): Boolean? {
        return getScalar(value, ScalarType.BOOL.schema) { it.asBoolean() }
    }

    override fun asJson(): Any? {
        return getScalar(value, ScalarType.JSON.schema) { it }
    }

    override fun asRaw(): Any? {
        return getScalar(value, ScalarType.RAW.schema) { it }
    }

    override fun asBin(): Any? {
        return getScalar(value, ScalarType.BIN.schema) { it }
    }

    override fun getType(): EntityRef {
        val type = if (value.has("_type")) {
            value.path("_type")
        } else {
            value.path("_etype")
        }
        val res = type.path(ScalarType.ID.schema)
        if (res.isNull || res.isMissingNode) {
            return EntityRef.EMPTY
        }
        return EntityRef.valueOf(res.asText())
    }
}
