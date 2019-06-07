package ru.citeck.ecos.records2.request.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;

public class MutationBody extends RecordsMutation {

    private boolean isSingleRecord = false;

    void setRecord(RecordMeta record) {
        isSingleRecord = true;
        getRecords().add(record);
    }

    @JsonIgnore
    public boolean isSingleRecord() {
        return isSingleRecord;
    }
}
