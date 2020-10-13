package ru.citeck.ecos.records2.request.rest;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody;

public class MutationBody extends RecordsMutation {

    @Getter @Setter
    private MutateBody v1Body;

    private boolean isSingleRecord = false;

    void setRecord(RecordMeta record) {
        isSingleRecord = true;
        getRecords().add(record);
    }

    @JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isSingleRecord() {
        return isSingleRecord;
    }
}
