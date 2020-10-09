package ru.citeck.ecos.records3.rest;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.record.request.msg.RequestMsg;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class QueryResp {

    @Getter @Setter private String queryId;
    @Getter @Setter private List<RecordAtts> records;
    @Getter @Setter private Map<String, String> attributes;
    //todo
    @Getter @Setter private List<RequestMsg> messages;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryResp queryBody = (QueryResp) o;
        return Objects.equals(records, queryBody.records)
            && Objects.equals(attributes, queryBody.attributes)
            && Objects.equals(attributes, queryBody.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            queryId,
            records,
            attributes
        );
    }

    @Override
    public String toString() {
        return "QueryResp{"
            + "records=" + records
            + ", queryId=" + queryId
            + ", attributes=" + attributes
            + '}';
    }
}
