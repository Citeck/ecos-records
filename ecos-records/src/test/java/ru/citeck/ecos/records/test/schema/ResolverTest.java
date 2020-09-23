package ru.citeck.ecos.records.test.schema;

import lombok.Data;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records2.meta.schema.resolver.AttSchemaResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResolverTest {

    private final RecordsServiceFactory factory = new RecordsServiceFactory();
    private final AttSchemaResolver resolver = new AttSchemaResolver(factory);
    private final AttSchemaReader reader = new AttSchemaReader();

    @Test
    void test() {

        testAtt(new TestClass(), "inner?json", "{\"key\":\"value\"}");
        testAtt(new TestClass(), "inner{.disp,innerField}", "{\".disp\":\"InnerDisp\", \"innerField\":\"inner\"}");
        testAtt(new TestClass(), "field0", "abc");
        testAtt(new TestClass(), "inner.innerField", "inner");
        testAtt(new TestClass(), "inner?disp", "InnerDisp");

        testAtt(new TestClass(), "inner{aa:.disp,innerField}", "{\"aa\":\"InnerDisp\", \"innerField\":\"inner\"}");
        testAtt(new TestClass(), "inner{aa:.disp,bb:innerField}", "{\"aa\":\"InnerDisp\", \"bb\":\"inner\"}");

        TestClass test = new TestClass();
        testAtt(test, "inner{a:someValue,b:someValue,c:someValue}", "{" +
            "\"a\":\"someValue\"," +
            "\"b\":\"someValue\"," +
            "\"c\":\"someValue\"}"
        );
        assertEquals(1, test.getInner().getSomeValueCounter());

        testAtt(new TestClass(), "inner[]?json", "[ {\"key\":\"value\"} ]");

    }

    private void testAtt(Object value, String att, Object expected) {
        assertEquals(DataValue.create(expected), resolver.resolve(value, reader.read(att)));
    }

    @Data
    public static class TestClass {
        private String field0 = "abc";
        private InnerTest inner = new InnerTest();
    }

    @Data
    public static class InnerTest {

        private String innerField = "inner";

        @Getter
        private int someValueCounter = 0;

        @MetaAtt(".disp")
        public String getDisplay() {
            return "InnerDisp";
        }

        @MetaAtt(".json")
        public ObjectData getJson() {
            return ObjectData.create("{\"key\":\"value\"}");
        }

        public String getSomeValue() {
            someValueCounter++;
            return "someValue";
        }
    }
}
