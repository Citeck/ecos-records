package ru.citeck.ecos.records2.request.mutation;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.result.RecordsResult;

public class RecordsMutResult extends RecordsResult<RecordMeta> {

    public RecordsMutResult() {
    }

    public RecordsMutResult(RecordsResult<RecordMeta> other) {
        super(other);
    }
}
