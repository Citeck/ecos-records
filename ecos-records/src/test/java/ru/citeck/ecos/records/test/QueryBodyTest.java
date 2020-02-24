package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.request.rest.QueryBody;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryBodyTest {

    @Test
    void test() throws JsonProcessingException {

        QueryBody body0 = Json.getMapper().read("{\"attributes\":{\"att0\":\"att0\",\"att1\":\"att1\"}}", QueryBody.class);
        QueryBody body1 = Json.getMapper().convert("{\"attributes\":[\"att0\",\"att1\"]}", QueryBody.class);

        assertEquals(body0, body1);
        assertEquals("att0", body0.getAttributes().get("att0"));
        assertEquals("att0", body1.getAttributes().get("att0"));

        QueryBody body2 = Json.getMapper().convert("{\"attribute\":\"test\"}", QueryBody.class);
        assertEquals(1, body2.getAttributes().size());
        assertEquals("test", body2.getAttributes().values().stream().findFirst().orElse(null));

        QueryBody body3 = Json.getMapper().convert("{\"foreach\":[\"test0\", \"test1\"], \"query\":{\"query\": \"q\"}}", QueryBody.class);
        assertEquals(2, body3.getForeach().size());
        assertEquals(new DataValue("test0"), body3.getForeach().get(0));

        ObjectMapper mapper = new ObjectMapper();

        QueryBody body = new QueryBody();
        String strBodyFromUtils = Json.getMapper().toString(body);
        String strBodyFromMapper = mapper.writeValueAsString(body);

        assertEquals(strBodyFromMapper, strBodyFromUtils);
    }
}
