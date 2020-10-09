package ru.citeck.ecos.records3.record.operation.mutate;

import lombok.Data;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.record.request.error.RecordError;
import ru.citeck.ecos.records3.record.request.msg.RequestMsg;

import java.util.ArrayList;
import java.util.List;

@Data
public class MutateResp {

    private List<RecordAtts> records = new ArrayList<>();
    //todo
    private List<RequestMsg> messages = new ArrayList<>();
    private List<RecordError> errors = new ArrayList<>();

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
