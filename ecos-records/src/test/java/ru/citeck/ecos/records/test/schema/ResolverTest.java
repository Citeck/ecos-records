package ru.citeck.ecos.records.test.schema;

import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records2.meta.schema.resolver.AttSchemaResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResolverTest {

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        AttSchemaResolver resolver = new AttSchemaResolver(factory);
        AttSchemaReader reader = new AttSchemaReader();

        assertEquals(DataValue.createStr("inner"), resolver.resolve(new TestClass(), reader.readAttribute("inner.innerField")));
        assertEquals(DataValue.createStr("abc"), resolver.resolve(new TestClass(), reader.readAttribute("field0")));
    }

    @Data
    public static class TestClass {
        private String field0 = "abc";
        private InnerTest inner = new InnerTest();
    }

    @Data
    public static class InnerTest {
        private String innerField = "inner";
    }
}
