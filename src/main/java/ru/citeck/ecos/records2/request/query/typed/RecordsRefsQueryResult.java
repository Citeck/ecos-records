package ru.citeck.ecos.records2.request.query.typed;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to deserialize query result with RecordRefs
 */
public class RecordsRefsQueryResult extends RecordsQueryResult<RecordRef> {

    public RecordsRefsQueryResult() {
    }

    public RecordsRefsQueryResult(RecordsRefsQueryResult other) {
        super(other);
    }

    public RecordsRefsQueryResult addSourceId(String sourceId) {
        List<RecordRef> records = new ArrayList<>();
        for (RecordRef ref : getRecords()) {
            records.add(RecordRef.create(sourceId, ref.toString()));
        }
        setRecords(records);
        return this;
    }
}
