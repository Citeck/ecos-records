package ru.citeck.ecos.records3.rest.v1.query;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import ecos.com.fasterxml.jackson210.annotation.JsonSetter;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import ecos.com.fasterxml.jackson210.databind.node.TextNode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.rest.v1.RequestBody;

import java.util.*;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class QueryBody extends RequestBody {

    @Nullable
    private List<RecordRef> records;
    @Nullable
    private RecordsQuery query;
    @Nullable
    private JsonNode attributes;

    private boolean rawAtts;

    public void setRecord(RecordRef record) {
        if (records == null) {
            records = new ArrayList<>();
        }
        records.add(record);
    }

    public void setAttribute(String attribute) {
        ObjectNode objNode = Json.getMapper().newObjectNode();
        objNode.set(attribute, TextNode.valueOf(attribute));
        this.attributes = objNode;
    }

    @JsonSetter
    @com.fasterxml.jackson.annotation.JsonSetter
    public void setAttributes(Object attributes) {
        JsonNode node = Json.getMapper().toJson(attributes);
        if (node.isArray()) {
            ObjectNode objNode = Json.getMapper().newObjectNode();
            node.forEach(n -> objNode.set(n.asText(), n));
            this.attributes = objNode;
        } else {
            this.attributes = node;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryBody queryBody = (QueryBody) o;
        return Objects.equals(records, queryBody.records)
            && Objects.equals(query, queryBody.query)
            && Objects.equals(attributes, queryBody.attributes)
            && Objects.equals(rawAtts, queryBody.rawAtts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            records,
            query,
            attributes,
            rawAtts
        );
    }
}
