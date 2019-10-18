package ru.citeck.ecos.records2.request.query.typed;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.result.RecordsResult;

/**
 * Used to deserialize records meta result.
 */
public class RecordsMetaResult extends RecordsResult<RecordMeta> {

    public RecordsMetaResult() {
    }

    public RecordsMetaResult(RecordsMetaResult other) {
        super(other);
    }
}
