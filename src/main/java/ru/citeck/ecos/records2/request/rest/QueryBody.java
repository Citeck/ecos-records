package ru.citeck.ecos.records2.request.rest;

import com.fasterxml.jackson.databind.JsonNode;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;

import java.util.ArrayList;
import java.util.List;

public class QueryBody {

    private List<RecordRef> records;
    private RecordsQuery query;

    /**
     * List<String> or Map<String, String>
     */
    private JsonNode attributes;
    private String schema;

    private boolean isSingleRecord = false;

    @Override
    public String toString() {
        return "Request{" + query + '}';
    }

    public void setRecord(RecordRef record) {
        if (records == null) {
            records = new ArrayList<>();
        }
        isSingleRecord = true;
        records.add(record);
    }

    public List<RecordRef> getRecords() {
        return records;
    }

    public void setRecords(List<RecordRef> records) {
        this.records = records;
    }

    public RecordsQuery getQuery() {
        return query;
    }

    public void setQuery(RecordsQuery query) {
        this.query = query;
    }

    public JsonNode getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonNode attributes) {
        this.attributes = attributes;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean isSingleRecord() {
        return isSingleRecord;
    }
}
