package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaValueTest extends LocalRecordsDAO
                           implements RecordsMetaLocalDAO<Object> {

    private static final String SOURCE_ID = "test-source";

    private RecordsService recordsService;
    private String innerSchema;

    @Override
    public List<Object> getMetaValues(List<RecordRef> records) {
        return Collections.singletonList(new MetaVal(s -> innerSchema = s));
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        String testInnerSchema = "a:att(n:\"test\"){str},b:att(n:\"number\"){num}";

        String schema = "str,disp,id,has(n:\"One\"),num,bool,json,schema:att(n:\"schema\"){" + testInnerSchema + "}";
        List<RecordRef> records = Collections.singletonList(RecordRef.create(SOURCE_ID, "test"));
        RecordsResult<RecordMeta> result = recordsService.getMeta(records, schema);

        RecordMeta meta = result.getRecords().get(0);

        assertEquals(MetaVal.DISP_VALUE, meta.getAttribute("disp", ""));
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("str", ""));
        assertEquals(MetaVal.DOUBLE_VALUE, meta.getAttribute("num", 0.0));
        assertEquals(MetaVal.BOOL_VALUE, meta.getAttribute("bool", false));
        assertEquals(true, meta.getAttribute("has", false));
        assertEquals(MetaVal.JSON_VALUE, meta.getAttribute("json"));
        assertEquals(MetaVal.ID_VALUE, meta.getAttribute("id", ""));

        assertEquals(testInnerSchema, innerSchema);
    }

    public static class MetaVal implements MetaValue {

        static String STRING_VALUE = "STR_VALUE";
        static String DISP_VALUE = "DISP_VALUE";
        static List<String> HAS_VARIANTS = Arrays.asList("One", "Two");
        static Double DOUBLE_VALUE = 99.0;
        static Boolean BOOL_VALUE = true;
        static JsonNode JSON_VALUE = JsonNodeFactory.instance.objectNode().with("Test").put("prop", "value");
        static String ID_VALUE = "SOME_ID";

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
        public Object getAttribute(String name, MetaField field) {
            if (name.equals("schema")) {
                schemaConsumer.accept(field.getInnerSchema());
            }
            return null;
        }
    }
}
