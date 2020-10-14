package ru.citeck.ecos.records3.rest.v1.query;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import ecos.com.fasterxml.jackson210.annotation.JsonSetter;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.query.RecordsQuery;
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
    private Map<String, String> attributes;

    private boolean rawAtts;

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
}
