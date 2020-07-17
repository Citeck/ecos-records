package ru.citeck.ecos.records.test;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaValueTest extends LocalRecordsDao
                           implements LocalRecordsMetaDao<Object> {

    private static final String SOURCE_ID = "test-source";

    private RecordsService recordsService;
    private String innerSchema;

    @NotNull
    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
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

        String testInnerSchema = "a:att(n:\"test\"){str},b:att(n:\"number\"){num}";

        String schema = "str," +
                        "disp," +
                        "id," +
                        "has(n:\"One\")," +
                        "num," +
                        "bool," +
                        "json," +
                        "schema:att(n:\"schema\"){" + testInnerSchema + "}," +
                        "asNum:as(n:\"num\"){num}," +
                        "asStr:as(n:\"str\"){str}," +
                        "date:att(n:\"date\"){str}";

        List<RecordRef> records = Collections.singletonList(RecordRef.create(SOURCE_ID, "test"));
        RecordsResult<RecordMeta> result = recordsService.getMeta(records, schema);

        RecordMeta meta = result.getRecords().get(0);

        assertEquals(MetaVal.DISP_VALUE, meta.getAttribute("disp", ""));
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("str", ""));
        assertEquals(MetaVal.DOUBLE_VALUE, meta.getAttribute("num", 0.0));
        assertEquals(MetaVal.BOOL_VALUE, meta.getAttribute("bool", false));
        assertEquals(true, meta.getAttribute("has", false));
        assertEquals(DataValue.create(MetaVal.JSON_VALUE), meta.getAttribute("json"));
        assertEquals(MetaVal.ID_VALUE, meta.getAttribute("id", ""));
        assertEquals(MetaVal.INT_VALUE, meta.getAttribute("asNum").get("num").asInt(0));
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("asStr").get("str").asText());

        String format = "yyyyy.MMMMM.dd GGG hh:mm aaa";
        String targetDate = (new SimpleDateFormat(format)).format(MetaVal.DATE_VALUE);
        assertEquals(targetDate, meta.fmtDate("/date/str", format, "-"));
        assertEquals(new Date((MetaVal.DATE_VALUE.getTime() / 1000) * 1000), meta.getDateOrNull("/date/str"));
        assertEquals("--", meta.fmtDate("date1", format, "--"));

        assertEquals(testInnerSchema, innerSchema);
    }

    public static class MetaVal implements MetaValue {

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
        public boolean has(String name) {
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
        public Object getAs(String type) {
            switch (type) {
                case "num":
                    return getDouble();
                case "str":
                    return getString();
            }
            return null;
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            if (name.equals("schema")) {
                schemaConsumer.accept(field.getInnerSchema());
            }
            if (name.equals("date")) {
                return DATE_VALUE;
            }
            return null;
        }
    }
}
