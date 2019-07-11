package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.request.error.RecordsError;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonMappingTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testRecordsError() throws IOException {

        RecordsError error = new RecordsError();
        error.setMsg("Test");
        error.setType("Type test");

        String json = mapper.writeValueAsString(error);
        RecordsError error2 = mapper.readValue(json, RecordsError.class);

        assertEquals(error, error2);
    }
}
