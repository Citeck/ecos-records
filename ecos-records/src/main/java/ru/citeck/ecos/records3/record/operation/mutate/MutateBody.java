package ru.citeck.ecos.records3.record.operation.mutate;

import lombok.Data;
import ru.citeck.ecos.records3.RecordAtts;

import java.util.ArrayList;
import java.util.List;

@Data
public class MutateBody {

    private boolean debug = false;
    private List<RecordAtts> records = new ArrayList<>();

    void setRecord(RecordAtts record) {
        this.records.add(record);
    }

    public List<RecordAtts> getRecords() {
        return records;
    }

    public void setRecords(List<RecordAtts> records) {
        if (records != null) {
            this.records = new ArrayList<>(records);
        } else {
            this.records.clear();
        }
    }

    public void addRecord(RecordAtts meta) {
        this.records.add(meta);
    }
}
