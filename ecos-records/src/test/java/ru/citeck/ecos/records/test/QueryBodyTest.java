package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.attributes.AttValue;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.utils.JsonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryBodyTest {

    @Test
    void test() {

        QueryBody body0 = JsonUtils.read("{\"attributes\":{\"att0\":\"att0\",\"att1\":\"att1\"}}", QueryBody.class);
        QueryBody body1 = JsonUtils.convert("{\"attributes\":[\"att0\",\"att1\"]}", QueryBody.class);

        assertEquals(body0, body1);
        assertEquals("att0", body0.getAttributes().get("att0"));
        assertEquals("att0", body1.getAttributes().get("att0"));

        QueryBody body2 = JsonUtils.convert("{\"attribute\":\"test\"}", QueryBody.class);
        assertEquals(1, body2.getAttributes().size());
        assertEquals("test", body2.getAttributes().values().stream().findFirst().orElse(null));

        QueryBody body3 = JsonUtils.convert("{\"foreach\":[\"test0\", \"test1\"], \"query\":{\"query\": \"q\"}}", QueryBody.class);
        assertEquals(2, body3.getForeach().size());
        assertEquals(new AttValue("test0"), body3.getForeach().get(0));
    }
}
