package ru.citeck.ecos.records3.source.dao.local;

import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.source.dao.RecordsDao;

import java.util.HashMap;
import java.util.Map;

public class RecordsDaoBuilder {

    private String id;
    private final Map<RecordRef, Object> records = new HashMap<>();

    private RecordsDaoBuilder() {
    }

    public static RecordsDaoBuilder create(String id) {
        return new RecordsDaoBuilder().setId(id);
    }

    public RecordsDaoBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public RecordsDaoBuilder addRecord(String recordRef, Object value) {
        addRecord(RecordRef.valueOf(recordRef), value);
        return this;
    }

    public RecordsDaoBuilder addRecord(RecordRef recordRef, Object value) {
        this.records.put(recordRef, value);
        return this;
    }

    public RecordsDaoBuilder addRecords(Map<RecordRef, Object> values) {
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
