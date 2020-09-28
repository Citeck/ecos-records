package ru.citeck.ecos.records.test.schema;

import lombok.Data;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.ScriptUtils;
import ru.citeck.ecos.commons.utils.func.UncheckedBiFunction;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.operation.meta.schema.resolver.AttResolver;
import ru.citeck.ecos.records3.source.common.AttMixin;
import ru.citeck.ecos.records3.source.common.AttValueCtx;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResolverTest {

    private final RecordsServiceFactory factory = new RecordsServiceFactory();
    private final AttResolver resolver = new AttResolver(factory);
    private final AttSchemaReader reader = new AttSchemaReader();

    @Test
    void test() {

        TestClass dto = new TestClass();

        testAtt(dto, "inner?json", "{\"key\":\"value\"}");
        testAtt(dto, "inner{.disp,innerField}", "{\".disp\":\"InnerDisp\", \"innerField\":\"inner\"}");
        testAtt(dto, "field0", "abc");
        testAtt(dto, "inner.innerField", "inner");
        testAtt(dto, "inner?disp", "InnerDisp");

        testAtt(dto, "inner{aa:.disp,innerField}", "{\"aa\":\"InnerDisp\", \"innerField\":\"inner\"}");
        testAtt(dto, "inner{aa:.disp,bb:innerField}", "{\"aa\":\"InnerDisp\", \"bb\":\"inner\"}");

        testAtt(dto, "inner[]?json", "[ {\"key\":\"value\"} ]");

        testAtt(dto, ".disp", dto.toString());
        testAtt(dto, "?disp", dto.toString());
    }

    @Test
    void mixinTest() {

        TestClass dto = new TestClass();

        testAtt(dto, "?disp", dto.toString());
        testAtt(dto, "?disp", dto.toString() + "-postfix",
            createMixin("?disp", (n, ctx) -> ctx.getAtt("?disp").asText() + "-postfix"));

        testAtt(dto, "inner{aa:?disp,bb:?json,cc:.disp,innerField}", "{" +
            "\"aa\":\"CUSTOM MIXIN VAL\"," +
            "\"bb\": {\"key\":\"value\"}," +
            "\"cc\":\"CUSTOM MIXIN VAL\"," +
            "\"innerField\": \"inner\"" +
            "}", createMixin("inner?disp", (n, ctx) -> "CUSTOM MIXIN VAL"));

        System.out.println(ScriptUtils.eval("return dto;", Collections.singletonMap("dto", dto)));
    }

    private AttMixin createMixin(String att, UncheckedBiFunction<String, AttValueCtx, Object> getter) {
        return new AttMixin() {
            @Override
            public Object getAtt(String name, AttValueCtx value) throws Exception {
                if (att.equals(name)) {
                    return getter.apply(name, value);
                }
                return null;
            }
            @Override
            public boolean isProvides(String path) {
                return att.equals(path);
            }
        };
    }

    @Test
    void testAttsCache() {

        TestClass test = new TestClass();
        testAtt(test, "inner{a:someValue,b:someValue,c:someValue}", "{" +
            "\"a\":\"someValue\"," +
            "\"b\":\"someValue\"," +
            "\"c\":\"someValue\"}"
        );
        assertEquals(1, test.getInner().getSomeValueCounter());
    }

    private void testAtt(Object value, String att, Object expected, AttMixin... mixins) {
        assertEquals(DataValue.create(expected), resolver.resolve(value, reader.read(att), Arrays.asList(mixins)));
    }

    private void testAtt(Object value, String att, Object expected) {
        assertEquals(DataValue.create(expected), resolver.resolve(value, reader.read(att)));
    }

    @Data
    public static class TestClass {

        private String field0 = "abc";
        private InnerTest inner = new InnerTest();

        @Override
        public String toString() {
            return "TestClass{\"" + field0 + "\"}";
        }
    }

    @Data
    public static class InnerTest {

        private String innerField = "inner";

        @Getter
        private int someValueCounter = 0;

        @MetaAtt("?disp")
        public String getDisplay() {
            return "InnerDisp";
        }

        @MetaAtt("?json")
        public ObjectData getJson() {
            return ObjectData.create("{\"key\":\"value\"}");
        }

        public String getSomeValue() {
            someValueCounter++;
            return "someValue";
        }
    }
}
