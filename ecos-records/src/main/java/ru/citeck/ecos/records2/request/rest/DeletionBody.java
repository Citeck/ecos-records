package ru.citeck.ecos.records2.request.rest;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;

public class DeletionBody extends RecordsDeletion {

    void setRecord(RecordRef record) {
        getRecords().add(record);
    }
}
