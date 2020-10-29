package ru.citeck.ecos.records2.request.rest;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody;

public class MutationBody extends RecordsMutation {

    private MutateBody v1Body;

    private boolean isSingleRecord = false;

    void setRecord(RecordMeta record) {
        isSingleRecord = true;
        getRecords().add(record);
    }

    public MutateBody getV1Body() {
        return v1Body;
    }

    public void setV1Body(MutateBody v1Body) {
        this.v1Body = v1Body;
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isSingleRecord() {
        return isSingleRecord;
    }
}
