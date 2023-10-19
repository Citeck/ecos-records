package ru.citeck.ecos.records2.request.query.typed;

import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to deserialize query result with RecordRefs.
 */
public class RecordsRefsQueryResult extends RecordsQueryResult<EntityRef> {

    public RecordsRefsQueryResult() {
    }

    public RecordsRefsQueryResult(RecordsRefsQueryResult other) {
        super(other);
    }

    public RecordsRefsQueryResult addSourceId(String sourceId) {
        List<EntityRef> records = new ArrayList<>();
        for (EntityRef ref : getRecords()) {
            records.add(EntityRef.create(sourceId, ref.toString()));
        }
        setRecords(records);
        return this;
    }
}
