package ru.citeck.ecos.records3.rest;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.record.request.msg.RequestMsg;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class QueryResp extends RecordsQueryRes<RecordAtts> {

    @Getter @Setter private List<RequestMsg> messages = new ArrayList<>();
    @Getter @Setter private int version = 1;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryResp queryBody = (QueryResp) o;
        return getRecords().equals(queryBody.getRecords());
    }

    @Override
    public int hashCode() {
        return getRecords().hashCode();
    }

    @Override
    public String toString() {
        return Json.getMapper().toString(this);
    }
}
