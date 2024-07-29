package ru.citeck.ecos.records3.rest.v1.query

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.rest.v1.RequestBody
import ru.citeck.ecos.records3.rest.v1.RestUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
open class QueryBody : RequestBody() {

    private var records: MutableList<EntityRef>? = null
    private var query: RecordsQuery? = null
    var attributes: DataValue = DataValue.NULL
    var rawAtts = false

    open fun setQuery(query: RecordsQuery?) {
        this.query = query
    }

    open fun getQuery(): RecordsQuery? {
        return query
    }

    fun setRecord(record: EntityRef) {
        records = (records ?: ArrayList()).apply { add(record) }
    }

    fun getRecords(): List<EntityRef>? {
        return records
    }

    fun setRecords(records: List<EntityRef>?) {
        this.records = records?.let { ArrayList(it) }
    }

    fun setAttribute(attribute: String) {
        val objNode: ObjectNode = Json.mapper.newObjectNode()
        objNode.set<JsonNode>(attribute, TextNode.valueOf(attribute))
        attributes = DataValue.create(objNode)
    }

    @JsonSetter
    fun setAttributes(attributes: Any?) {
        this.attributes = RestUtils.prepareReqAtts(attributes)
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
