package ru.citeck.ecos.records.test;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.operation.meta.schema.resolver.AttContext;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaValueTest extends AbstractRecordsDao
                           implements RecordsAttsDao {

    private static final String SOURCE_ID = "test-source";

    private RecordsService recordsService;
    private String innerSchema;

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return Collections.singletonList(new MetaVal(s -> innerSchema = s));
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        String testInnerSchema = "number:att(n:\"number\"){_u003F_num:num},test:att(n:\"test\"){_u003F_str:str}";

        Map<String, String> attributes = new HashMap<>();
        attributes.put("str", ".str");
        attributes.put("disp", ".disp");
        attributes.put("id", ".id");
        attributes.put("localId", ".localId");
        attributes.put("has", ".has(n:\"One\")");
        attributes.put("num", ".num");
        attributes.put("bool", ".bool");
        attributes.put(".json", ".json");
        attributes.put("schema", ".att(n:\"schema\"){" + testInnerSchema + "}");
        attributes.put("asNum", ".as(n:\"num\"){num}");
        attributes.put("asStr", ".as(n:\"str\"){str}");
        attributes.put("date", ".att(n:\"date\"){str}");

        List<RecordRef> records = Collections.singletonList(RecordRef.create(SOURCE_ID, "test"));
        List<RecordAtts> result = recordsService.getAtts(records, attributes);

        RecordAtts meta = result.get(0);

        assertEquals(MetaVal.DISP_VALUE, meta.getAttribute("disp", ""));
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("str", ""));
        assertEquals(MetaVal.DOUBLE_VALUE, meta.getAttribute("num", 0.0));
        assertEquals(MetaVal.BOOL_VALUE, meta.getAttribute("bool", false));
        assertEquals(true, meta.getAttribute("has", false));
        assertEquals(DataValue.create(MetaVal.JSON_VALUE), meta.getAttribute(".json"));
        assertEquals(records.get(0).toString(), meta.getAttribute("id", ""));
        assertEquals(MetaVal.INT_VALUE, meta.getAttribute("asNum").asInt(0));
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("asStr").asText());

        String format = "yyyyy.MMMMM.dd GGG hh:mm aaa";
        String targetDate = (new SimpleDateFormat(format)).format(MetaVal.DATE_VALUE);
        assertEquals(targetDate, meta.fmtDate("/date", format, "-"));
        assertEquals(new Date((MetaVal.DATE_VALUE.getTime() / 1000) * 1000), meta.getDateOrNull("/date"));
        assertEquals("--", meta.fmtDate("date1", format, "--"));

        assertEquals(testInnerSchema, innerSchema);
    }

    public static class MetaVal implements AttValue {

        static String STRING_VALUE = "STR_VALUE";
        static String DISP_VALUE = "DISP_VALUE";
        static List<String> HAS_VARIANTS = Arrays.asList("One", "Two");
        static double DOUBLE_VALUE = 99.0;
        static int INT_VALUE = (int) DOUBLE_VALUE;
        static Boolean BOOL_VALUE = true;
        static JsonNode JSON_VALUE = JsonNodeFactory.instance.objectNode().with("Test").put("prop", "value");
        static String ID_VALUE = "SOME_ID";
        static Date DATE_VALUE = new Date();

        private Consumer<String> schemaConsumer;

        public MetaVal(Consumer<String> schemaConsumer) {
            this.schemaConsumer = schemaConsumer;
        }

        @Override
        public String getString() {
            return STRING_VALUE;
        }

        @Override
        public String getDisplayName() {
            return DISP_VALUE;
        }

        @Override
        public String getId() {
            return ID_VALUE;
        }

        @Override
        public boolean has(@NotNull String name) {
            return HAS_VARIANTS.contains(name);
        }

        @Override
        public Double getDouble() {
            return DOUBLE_VALUE;
        }

        @Override
        public Boolean getBool() {
            return BOOL_VALUE;
        }

        @Override
        public Object getJson() {
            return JSON_VALUE;
        }

        @Override
        public Object getAs(@NotNull String type) {
            switch (type) {
                case "num":
                    return getDouble();
                case "str":
                    return getString();
            }
            return null;
        }

        @Override
        public Object getAtt(@NotNull String name) {
            if (name.equals("schema")) {
                schemaConsumer.accept(AttContext.getCurrentSchemaAttInnerStr());
            }
            if (name.equals("date")) {
                return DATE_VALUE;
            }
            return null;
        }
    }
}
