package ru.citeck.ecos.records2.request.rest;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody;

public class DeletionBody extends RecordsDeletion {

    private DeleteBody v1Body;

    public DeleteBody getV1Body() {
        return v1Body;
    }

    public void setV1Body(DeleteBody v1Body) {
        this.v1Body = v1Body;
    }

    void setRecord(RecordRef record) {
        getRecords().add(record);
    }
}
