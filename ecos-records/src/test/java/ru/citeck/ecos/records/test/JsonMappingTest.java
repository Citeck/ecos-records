package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.request.error.RecordsError;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonMappingTest {

    @Test
    void testRecordsError() {

        RecordsError error = new RecordsError();
        error.setMsg("Test");
        error.setType("Type test");

        String json = Json.getMapper().toString(error);
        RecordsError error2 = Json.getMapper().convert(json, RecordsError.class);

        assertEquals(error, error2);
    }
}
