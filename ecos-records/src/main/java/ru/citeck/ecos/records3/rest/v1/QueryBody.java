package ru.citeck.ecos.records3.rest.v1;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import ecos.com.fasterxml.jackson210.annotation.JsonSetter;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class QueryBody extends RequestBody {

    @Getter @Setter private List<RecordRef> records;
    @Getter @Setter private RecordsQuery query;
    @Getter @Setter private boolean rawAtts;

    @Getter private Map<String, String> attributes;

    public void setRecord(RecordRef record) {
        if (records == null) {
            records = new ArrayList<>();
        }
        records.add(record);
    }

    public void setAttribute(String attribute) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        this.attributes.put(attribute, attribute);
    }

    @JsonSetter
    @com.fasterxml.jackson.annotation.JsonSetter
    public void setAttributes(JsonNode attributes) {

        Map<String, String> result = new HashMap<>();

        if (attributes.isArray()) {
            for (int i = 0; i < attributes.size(); i++) {
                String field = attributes.get(i).asText();
                result.put(field, field);
            }
        } else {
            Iterator<String> names = attributes.fieldNames();
            while (names.hasNext()) {
                String fieldKey = names.next();
                result.put(fieldKey, attributes.get(fieldKey).asText());
            }
        }

        this.attributes = result;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = new HashMap<>();
        attributes.forEach(a -> this.attributes.put(a, a));
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
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

    @Override
    public String toString() {
        return Json.getMapper().toString(this);
    }
}
