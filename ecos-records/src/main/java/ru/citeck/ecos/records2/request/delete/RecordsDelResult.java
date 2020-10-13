package ru.citeck.ecos.records2.request.delete;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.result.RecordsResult;

@Deprecated
public class RecordsDelResult extends RecordsResult<RecordMeta> {

    public RecordsDelResult() {
    }

    public RecordsDelResult(RecordsResult<RecordMeta> other) {
        super(other);
    }
}
