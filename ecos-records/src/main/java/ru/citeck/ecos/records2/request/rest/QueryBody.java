package ru.citeck.ecos.records2.request.rest;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import ecos.com.fasterxml.jackson210.annotation.JsonSetter;
import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;

import java.util.*;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class QueryBody {

    public static final String SINGLE_ATT_KEY = "a";

    @Nullable
    private List<RecordRef> records;
    @Nullable
    private RecordsQuery query;

    private String schema;
    @Getter private Map<String, String> attributes;

    private boolean isSingleRecord = false;
    private boolean isSingleAttribute = false;

    private ru.citeck.ecos.records3.rest.v1.query.QueryBody v1Body;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Nullable
    public RecordsQuery getQuery() {
        return query;
    }

    public void setQuery(@Nullable RecordsQuery query) {
        this.query = query;
    }

    public ru.citeck.ecos.records3.rest.v1.query.QueryBody getV1Body() {
        return v1Body;
    }

    public void setV1Body(ru.citeck.ecos.records3.rest.v1.query.QueryBody v1Body) {
        this.v1Body = v1Body;
    }

    @Nullable
    public List<RecordRef> getRecords() {
        return records;
    }

    public void setRecords(@Nullable List<RecordRef> records) {
        this.records = records;
    }

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
    @com.fasterxml.jackson.annotation.JsonSetter
    public void setAttributes(@Nullable JsonNode attributes) {

        Map<String, String> result = new HashMap<>();

        if (attributes != null) {
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
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isSingleRecord() {
        return isSingleRecord && records != null && records.size() == 1;
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
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
            && Objects.equals(attributes, queryBody.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            records,
            query,
            schema,
            attributes,
            isSingleRecord,
            isSingleAttribute
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
            + '}';
    }
}
