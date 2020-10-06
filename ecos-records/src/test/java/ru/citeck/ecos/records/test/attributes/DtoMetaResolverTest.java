package ru.citeck.ecos.records.test.attributes;

import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.operation.meta.schema.write.AttSchemaWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DtoMetaResolverTest {

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        DtoSchemaReader dtoMetaResolver = factory.getDtoSchemaReader();
        AttSchemaWriter writer = factory.getAttSchemaWriter();

        List<SchemaRootAtt> attributes = dtoMetaResolver.read(TestDto.class);
        Map<String, String> atts = writer.writeToMap(attributes);

        Map<String, String> expected = new HashMap<>();
        expected.put("model", ".att(n:\"model\"){json}");

        assertEquals(expected, atts);
    }

    @Data
    public static class TestDto {
        private Map<String, String> model;
    }
}
