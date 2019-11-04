package ru.citeck.ecos.records2.request.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryBody {

    public static final String SINGLE_ATT_KEY = "a";

    @Getter @Setter private List<RecordRef> records;
    @Getter @Setter private RecordsQuery query;
    @Getter @Setter private List<JsonNode> foreach;

    @Getter @Setter private String schema;
    @Getter private Map<String, String> attributes;

    private boolean isSingleRecord = false;
    private boolean isSingleAttribute = false;

    public void setRecord(RecordRef record) {
        if (records == null) {
            records = new ArrayList<>();
        }
        isSingleRecord = true;
        records.add(record);
    }

    public void setAttribute(String attribute) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        this.attributes.put(SINGLE_ATT_KEY, attribute);
        isSingleAttribute = true;
    }

    @JsonSetter
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

    @JsonIgnore
    public boolean isSingleRecord() {
        return isSingleRecord && records != null && records.size() == 1;
    }

    @JsonIgnore
    public boolean isSingleAttribute() {
        return isSingleAttribute && attributes != null && attributes.size() == 1;
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
        return isSingleRecord == queryBody.isSingleRecord
            && isSingleAttribute == queryBody.isSingleAttribute
            && Objects.equals(records, queryBody.records)
            && Objects.equals(query, queryBody.query)
            && Objects.equals(schema, queryBody.schema)
            && Objects.equals(attributes, queryBody.attributes)
            && Objects.equals(foreach, queryBody.foreach);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            records,
            query,
            schema,
            attributes,
            isSingleRecord,
            isSingleAttribute,
            foreach
        );
    }

    @Override
    public String toString() {
        return "QueryBody{"
            + "records=" + records
            + ", query=" + query
            + ", schema='" + schema + '\''
            + ", attributes=" + attributes
            + ", isSingleRecord=" + isSingleRecord
            + ", isSingleAttribute=" + isSingleAttribute
            + ", foreach=" + foreach
            + '}';
    }
}