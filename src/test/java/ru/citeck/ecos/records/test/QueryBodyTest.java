package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.request.rest.QueryBody;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryBodyTest {

    @Test
    void test() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();

        QueryBody body0 = objectMapper.readValue("{\"attributes\":{\"att0\":\"att0\",\"att1\":\"att1\"}}", QueryBody.class);
        QueryBody body1 = objectMapper.readValue("{\"attributes\":[\"att0\",\"att1\"]}", QueryBody.class);

        assertEquals(body0, body1);
        assertEquals("att0", body0.getAttributes().get("att0"));
        assertEquals("att0", body1.getAttributes().get("att0"));

        QueryBody body2 = objectMapper.readValue("{\"attribute\":\"test\"}", QueryBody.class);
        assertEquals(1, body2.getAttributes().size());
        assertEquals("test", body2.getAttributes().values().stream().findFirst().orElse(null));
    }
}
