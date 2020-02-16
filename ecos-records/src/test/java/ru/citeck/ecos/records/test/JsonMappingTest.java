package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.request.error.RecordsError;
import ru.citeck.ecos.records2.utils.JsonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonMappingTest {

    @Test
    void testRecordsError() {

        RecordsError error = new RecordsError();
        error.setMsg("Test");
        error.setType("Type test");

        String json = JsonUtils.toString(error);
        RecordsError error2 = JsonUtils.convert(json, RecordsError.class);

        assertEquals(error, error2);
    }
}
