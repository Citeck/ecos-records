package ru.citeck.ecos.records3.record.operation.query.dto.typed;

import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to deserialize query result with RecordRefs.
 */
public class RecordsRefsQueryRes extends RecordsQueryRes<RecordRef> {

    public RecordsRefsQueryRes() {
    }

    public RecordsRefsQueryRes(RecordsRefsQueryRes other) {
        super(other);
    }

    public RecordsRefsQueryRes addSourceId(String sourceId) {
        List<RecordRef> records = new ArrayList<>();
        for (RecordRef ref : getRecords()) {
            records.add(RecordRef.create(sourceId, ref.toString()));
        }
        setRecords(records);
        return this;
    }
}
