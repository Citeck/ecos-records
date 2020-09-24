package ru.citeck.ecos.records.test.attributes;

import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.meta.schema.read.DtoSchemaResolver;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DtoMetaResolverTest {

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        DtoSchemaResolver dtoMetaResolver = factory.getDtoMetaResolver();

        Map<String, String> attributes = new HashMap<>(dtoMetaResolver.getAttributes(TestDto.class));

        Map<String, String> expected = new HashMap<>();
        expected.put("model", ".att(n:\"model\"){json}");

        assertEquals(expected, attributes);
    }

    @Data
    public static class TestDto {
        private Map<String, String> model;
    }
}
