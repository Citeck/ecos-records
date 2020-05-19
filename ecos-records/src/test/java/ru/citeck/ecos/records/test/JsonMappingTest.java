package ru.citeck.ecos.records.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

        TestWithEnum test = new TestWithEnum(TestEnum.FIRST);
        assertEquals(test, Json.getMapper().copy(test));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestWithEnum {
        private TestEnum enumField;
    }

    public enum TestEnum { FIRST, SECOND }
}
