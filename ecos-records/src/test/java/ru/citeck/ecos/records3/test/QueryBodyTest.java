package ru.citeck.ecos.records3.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.rest.v1.query.QueryBody;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryBodyTest {

    @Test
    void test() throws JsonProcessingException {

        QueryBody body0 = Json.getMapper().read("{\"attributes\":{\"att0\":\"att0\",\"att1\":\"att1\"}}", QueryBody.class);
        QueryBody body1 = Json.getMapper().convert("{\"attributes\":[\"att0\",\"att1\"]}", QueryBody.class);

        assertEquals(body0, body1);
        assertEquals("att0", body0.getAttributes().get("att0").asText());
        assertEquals("att0", body1.getAttributes().get("att0").asText());

        QueryBody body2 = Json.getMapper().convert("{\"attribute\":\"test\"}", QueryBody.class);
        assertEquals(1, body2.getAttributes().size());
        assertEquals("test", body2.getAttributes().get("test").asText());

        ObjectMapper mapper = new ObjectMapper();

        QueryBody body = new QueryBody();
        String strBodyFromUtils = Json.getMapper().toString(body);
        String strBodyFromMapper = mapper.writeValueAsString(body);

        assertEquals(DataValue.create(strBodyFromMapper), DataValue.create(strBodyFromUtils));
    }
}
