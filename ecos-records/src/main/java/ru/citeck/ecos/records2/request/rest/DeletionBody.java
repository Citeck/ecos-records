package ru.citeck.ecos.records2.request.rest;

import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody;

public class DeletionBody extends RecordsDeletion {

    @Getter @Setter
    private DeleteBody v1Body;

    void setRecord(RecordRef record) {
        getRecords().add(record);
    }
}
