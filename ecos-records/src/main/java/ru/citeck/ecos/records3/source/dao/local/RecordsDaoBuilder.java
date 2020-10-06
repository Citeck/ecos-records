package ru.citeck.ecos.records3.source.dao.local;

import ru.citeck.ecos.records3.source.dao.RecordsDao;

import java.util.HashMap;
import java.util.Map;

public class RecordsDaoBuilder {

    private String id;
    private final Map<String, Object> records = new HashMap<>();

    private RecordsDaoBuilder() {
    }

    public static RecordsDaoBuilder create(String id) {
        return new RecordsDaoBuilder().setId(id);
    }

    public RecordsDaoBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public RecordsDaoBuilder addRecord(String recordId, Object value) {
        records.put(recordId, value);
        return this;
    }

    public RecordsDaoBuilder addRecords(Map<String, Object> values) {
        this.records.putAll(values);
        return this;
    }

    public RecordsDao build() {
        if (id == null) {
            throw new IllegalStateException("Id is a mandatory parameter");
        }
        InMemRecordsDao<Object> dao = new InMemRecordsDao<>(id);
        dao.setRecords(records);
        return dao;
    }
}
