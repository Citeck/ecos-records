package ru.citeck.ecos.records3.rest.v1.mutate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.rest.v1.RequestBody;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class MutateBody extends RequestBody {

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
