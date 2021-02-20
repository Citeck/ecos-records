package ru.citeck.ecos.records2.request.query.typed;

import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;

/**
 * Used to deserialize records meta result.
 */
public class RecordsMetaResult extends RecordsResult<RecordAtts> {

    public RecordsMetaResult() {
    }

    public RecordsMetaResult(RecordsMetaResult other) {
        super(other);
    }
}
