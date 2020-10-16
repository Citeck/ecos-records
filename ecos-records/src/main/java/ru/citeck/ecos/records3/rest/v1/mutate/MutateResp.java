package ru.citeck.ecos.records3.rest.v1.mutate;

import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.rest.v1.RequestResp;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MutateResp extends RequestResp {

    @Getter
    private List<RecordAtts> records = new ArrayList<>();

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
