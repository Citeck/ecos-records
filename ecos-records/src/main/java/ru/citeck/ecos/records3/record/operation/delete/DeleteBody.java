package ru.citeck.ecos.records3.record.operation.delete;

import lombok.Data;
import ru.citeck.ecos.records3.RecordRef;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeleteBody {

    private boolean debug = false;
    private List<RecordRef> records = new ArrayList<>();

    public void setRecords(List<RecordRef> records) {
        if (records != null) {
            this.records = new ArrayList<>(records);
        } else {
            this.records.clear();
        }
    }

    void setRecord(RecordRef record) {
        this.records.add(record);
    }
}
