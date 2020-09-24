package ru.citeck.ecos.records3.record.operation.query.typed;

import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to deserialize query result with RecordRefs.
 */
public class RecsRefsQueryRes extends RecsQueryRes<RecordRef> {

    public RecsRefsQueryRes() {
    }

    public RecsRefsQueryRes(RecsRefsQueryRes other) {
        super(other);
    }

    public RecsRefsQueryRes addSourceId(String sourceId) {
        List<RecordRef> records = new ArrayList<>();
        for (RecordRef ref : getRecords()) {
            records.add(RecordRef.create(sourceId, ref.toString()));
        }
        setRecords(records);
        return this;
    }
}
