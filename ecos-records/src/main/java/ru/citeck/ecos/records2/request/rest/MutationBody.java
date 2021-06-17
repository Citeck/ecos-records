package ru.citeck.ecos.records2.request.rest;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody;
import ru.citeck.ecos.records3.security.HasSensitiveData;

import java.util.stream.Collectors;

public class MutationBody extends RecordsMutation implements HasSensitiveData<MutationBody> {

    private MutateBody v1Body;

    private boolean isSingleRecord = false;

    public MutationBody() {
    }

    public MutationBody(MutationBody other) {
        this.v1Body = other.v1Body != null ? new MutateBody(other.v1Body) : null;
        this.isSingleRecord = other.isSingleRecord;
    }

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

    @NotNull
    @Override
    public MutationBody withoutSensitiveData() {
        MutationBody newBody = new MutationBody(this);
        if (newBody.v1Body != null) {
            newBody.v1Body = newBody.v1Body.withoutSensitiveData();
        }
        newBody.setRecords(getRecords().stream()
            .map(RecordMeta::withoutSensitiveData)
            .collect(Collectors.toList()));
        return newBody;
    }
}
