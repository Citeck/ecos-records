package ru.citeck.ecos.records3.rest.v1.query

import ecos.com.fasterxml.jackson210.annotation.JsonInclude
import ecos.com.fasterxml.jackson210.annotation.JsonSetter
import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode
import ecos.com.fasterxml.jackson210.databind.node.TextNode
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.rest.v1.RequestBody
import java.util.*
import com.fasterxml.jackson.annotation.JsonInclude as JackJsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JackJsonInclude(JackJsonInclude.Include.NON_NULL)
class QueryBody : RequestBody() {

    private var records: MutableList<RecordRef>? = null
    var query: RecordsQuery? = null
    var attributes: DataValue = DataValue.NULL
    var rawAtts = false

    fun setRecord(record: RecordRef) {
        records = (records ?: ArrayList()).apply { add(record) }
    }

    fun getRecords(): List<RecordRef>? {
        return records
    }

    fun setRecords(records: List<RecordRef>?) {
        this.records = records?.let { ArrayList(it) }
    }

    fun setAttribute(attribute: String) {
        val objNode: ObjectNode = Json.mapper.newObjectNode()
        objNode.set<JsonNode>(attribute, TextNode.valueOf(attribute))
        attributes = DataValue.create(objNode)
    }

    @JsonSetter
    @com.fasterxml.jackson.annotation.JsonSetter
    fun setAttributes(attributes: Any?) {
        val node: JsonNode = Json.mapper.toJson(attributes)
        if (node.isArray) {
            val objNode: ObjectNode = Json.mapper.newObjectNode()
            node.forEach { n -> objNode.set<ObjectNode>(n.asText(), n) }
            this.attributes = DataValue.create(objNode)
        } else {
            this.attributes = DataValue.create(node)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val queryBody = other as? QueryBody ?: return false
        return (
            records == queryBody.records &&
                query == queryBody.query &&
                attributes == queryBody.attributes &&
                rawAtts == queryBody.rawAtts
            )
    }

    override fun hashCode(): Int {
        return Objects.hash(
            records,
            query,
            attributes,
            rawAtts
        )
    }
}