package ru.citeck.ecos.records3.record.op.mutate;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import lombok.Data;
import ru.citeck.ecos.records3.RecordMeta;

import java.util.ArrayList;
import java.util.List;

@Data
public class MutateBody {

    private boolean isSingleRecord = false;
    private List<RecordMeta> records = new ArrayList<>();

    void setRecord(RecordMeta record) {
        isSingleRecord = true;
        getRecords().add(record);
    }

    public List<RecordMeta> getRecords() {
        return records;
    }

    public void setRecords(List<RecordMeta> records) {
        isSingleRecord = true;
        if (records != null) {
            this.records = new ArrayList<>(records);
        } else {
            this.records.clear();
        }
    }

    public void addRecord(RecordMeta meta) {
        this.records.add(meta);
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isSingleRecord() {
        return isSingleRecord;
    }
}
