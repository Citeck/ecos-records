package ru.citeck.ecos.records2.request.query.typed;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to deserialize query result with RecordRefs
 */
public class RecordsMetaQueryResult extends RecordsQueryResult<RecordMeta> {

    public RecordsMetaQueryResult() {
    }

    public RecordsMetaQueryResult(RecordsMetaQueryResult other) {
        super(other);
    }

    public RecordsMetaQueryResult addSourceId(String sourceId) {
        for (RecordMeta record : getRecords()) {
            record.setId(new RecordRef(sourceId, record.getId().toString()));
        }
        return this;
    }
}
