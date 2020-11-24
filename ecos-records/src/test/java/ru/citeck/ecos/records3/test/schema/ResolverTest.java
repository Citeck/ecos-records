package ru.citeck.ecos.records3.test.schema;

import lombok.Data;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.func.UncheckedBiFunction;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttSchemaResolver;
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.ResolveArgs;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.op.atts.service.mixin.AttMixin;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValueCtx;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResolverTest {

    private final RecordsServiceFactory factory = new RecordsServiceFactory();
    private final AttSchemaResolver resolver = factory.getAttSchemaResolver();
    private final AttSchemaReader reader = factory.getAttSchemaReader();

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

      /*   testAtt(dto, "?disp", dto.toString());
        testAtt(dto, "?disp", dto.toString() + "-postfix",
            createMixin("?disp", (n, ctx) -> ctx.getAtt("?disp").asText() + "-postfix"));

        testAtt(dto, "inner{aa:?disp,bb:?json,cc:.disp,innerField}", "{" +
            "\"aa\":\"CUSTOM MIXIN VAL\"," +
            "\"bb\": {\"key\":\"value\"}," +
            "\"cc\":\"CUSTOM MIXIN VAL\"," +
            "\"innerField\": \"inner\"" +
            "}", createMixin("inner?disp", (n, ctx) -> "CUSTOM MIXIN VAL"));

        testAtt(dto, "inner?disp", "InnerDisp");
        testAtt(dto, "inner.innerField?str", "inner");
        testAtt(dto, "inner.innerField?disp", "inner");*/

        testAtt(dto, "inner.innerField{?disp,?str}", "{\"?disp\":\"inner-dispName\",\"?str\":\"inner\"}",
            createMixin("inner.innerField?disp", (n, ctx) -> ctx.getAtt("inner.innerField?str").asText() + "-dispName"));

        testAtt(dto, "inner.innerField?disp", "aa@bb",
            createMixin("inner.innerField?disp", (n, ctx) -> ctx.getRef()));
    }

    @Test
    void attProcTest() {

        TestClass dto = new TestClass();

        testAtt(dto, "number22!13", 13.0);
        testAtt(dto, "number?num!13", 11.0);
        testAtt(dto, "number22?num|or(13)", 13.0);

        testAtt(dto, "inner.unknown!inner.innerBoolField23?bool!false", false);

        testAtt(dto, "inner.unknown!inner.innerBoolField23?bool!'false'|cast('bool')", false);
        testAtt(dto, "inner.unknown!inner.innerBoolField23?bool!\"false\"|cast('bool')", false);

        testAtt(dto, "unknown!inner.innerField", "inner");
        testAtt(dto, "inner.innerField!unknown", "inner");
        testAtt(dto, "inner.innerField!unknown|presuf('prefix-', '-postfix')", "prefix-inner-postfix");
        testAtt(dto, "inner.innerField22!unknown|presuf('prefix-', '-postfix')", null);

        testAtt(dto, "inner.innerBoolField?bool!unknown", true);
        testAtt(dto, "inner.unknown?bool!inner.innerBoolField?bool", true);

        testAtt(dto, "inner.unknown?bool!inner.innerBoolField23?bool", null);

        testAtt(dto, "inner.innerField[]|presuf('prefix-')", "[\"prefix-inner\"]");
        testAtt(dto, "inner.innerFieldArr[]|join()", "inner0,inner1,inner2");
        testAtt(dto, "inner.innerFieldArr[]|join(', ')", "inner0, inner1, inner2");
        testAtt(dto, "inner.innerFieldArr[]|join('#')", "inner0#inner1#inner2");
        testAtt(dto, "inner.innerFieldArr[]|join('#')|presuf('[',']')", DataValue.createStr("[inner0#inner1#inner2]"));

        testAtt(dto, "number|fmt('0000.0')", "0011.0");
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
            public Collection<String> getProvidedAtts() {
                return Collections.singleton(att);
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

    private void testAtt(Object value, String att, Object expected) {
        testAtt(value, att, expected, new AttMixin[0]);
    }

    private void testAtt(Object value, String att, Object expected, AttMixin... mixins) {
        RequestContext.doWithCtx(factory, ctx -> {

            DataValue expValue;
            if (!(expected instanceof DataValue)) {
                expValue = DataValue.create(expected);
            } else {
                expValue = (DataValue) expected;
            }

            SchemaAtt parsedAtt = reader.read(att);
            Object result = resolver.resolve(ResolveArgs.create()
                .withValues(Collections.singletonList(value))
                .withAttribute(parsedAtt)
                .withMixins(Arrays.asList(mixins))
                .build()).get(0).get(parsedAtt.getAliasForValue());

            if (!(result instanceof DataValue)) {
                result = DataValue.create(result);
            }

            assertEquals(expValue, result);
            return null;
        });
    }

    @Data
    public static class TestClass {

        private String field0 = "abc";
        private InnerTest inner = new InnerTest();
        private int number = 11;

        @AttName("?id")
        public RecordRef getRef() {
            return RecordRef.create("aa", "bb");
        }

        @Override
        public String toString() {
            return "TestClass{\"" + field0 + "\"}";
        }
    }

    @Data
    public static class InnerTest {

        private String innerField = "inner";
        private Boolean innerBoolField = true;

        private List<String> innerFieldArr = Arrays.asList("inner0", "inner1", "inner2");

        @Getter
        private int someValueCounter = 0;

        @AttName("?disp")
        public String getDisplay() {
            return "InnerDisp";
        }

        @AttName("?json")
        public ObjectData getJson() {
            return ObjectData.create("{\"key\":\"value\"}");
        }

        public String getSomeValue() {
            someValueCounter++;
            return "someValue";
        }
    }
}
