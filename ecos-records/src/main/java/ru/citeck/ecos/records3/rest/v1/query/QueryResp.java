package ru.citeck.ecos.records3.rest.v1.query;

import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.rest.v1.RequestResp;

import java.util.*;

@Getter
@Setter
public class QueryResp extends RequestResp {

    private List<RecordAtts> records = new ArrayList<>();
    private boolean hasMore = false;
    private long totalCount = 0;

    public void setRecords(List<RecordAtts> records) {
        if (records != null) {
            this.records = new ArrayList<>(records);
        } else {
            this.records.clear();
        }
    }
}
