package ru.citeck.ecos.records2.test;

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
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDao;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaValueTest extends LocalRecordsDao
                           implements LocalRecordsMetaDao<Object>,
                                      LocalRecordsQueryDao {

    private static final String SOURCE_ID = "test-source";

    private RecordsService recordsService;

    @NotNull
    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
        return Collections.singletonList(new MetaVal());
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Override
    public RecordsQueryResult<RecordRef> queryLocalRecords(@NotNull RecordsQuery query) {
        return RecordsQueryResult.of(RecordRef.create(SOURCE_ID, "test"));
    }

    @Test
    void test() {

        String schema = "str," +
                        "disp," +
                        "id," +
                        "has(n:\"One\")," +
                        "num," +
                        "bool," +
                        "json," +
                        "asNum:as(n:\"num\"){num}," +
                        "asStr:as(n:\"str\"){str}," +
                        "date:att(n:\"date\"){str}," +
                        "thisAsTest0:str," +
                        "thisAsTest1:as(n:\"this\"){str}," +
                        "thisAsTest2:as(n:\"this\"){as(n:\"this\"){str}}," +
                        "thisAsTest3:as(n:\"this\"){as(n:\"this\"){as(n:\"this\"){str}}}," +
                        "thisAsTest4:as(n:\"this\"){as(n:\"this\"){as(n:\"this\"){as(n:\"this\"){str}}}}," +
                        "thisEdgeTest1:edge(n:\"this\"){val{str}}," +
                        "thisEdgeTest2:edge(n:\"this\"){val{edge(n:\"this\"){valAlias:val{str}}}}," +
                        "thisEdgeTest3:edge(n:\"this\"){val{edge(n:\"this\"){val{str}}}}," +
                        "thisEdgeTest4:edge(n:\"this\"){val{edge(n:\"this\"){val{edge(n:\"this\"){val{str}}}}}}," +
                        "thisEdgeTest5:edge(n:\"this\"){name}," +
                        "thisEdgeTest6:edge(n:\"this\"){" +
                            "val{" +
                                "edge(n:\"this\"){" +
                                    "valValue:val{" +
                                        "asStrAtt:att(n:\"strAtt\"){str}," +
                                        "asThisInner:as(n:\"this\"){" +
                                            "edge(n:\"this\"){" +
                                                "val{" +
                                                    "str" +
                                                "}" +
                                            "}" +
                                        "}," +
                                        "edge(n:\"this\"){" +
                                            "val{" +
                                                "str" +
                                            "}" +
                                        "}" +
                                    "}" +
                                    "name," +
                                    "strVal:val{att(n:\"strAtt\"){str}}" +
                                "}" +
                        "}}";

        List<RecordRef> records = Collections.singletonList(RecordRef.create(SOURCE_ID, "test"));
        RecordsResult<RecordMeta> result = recordsService.getMeta(records, schema);

        RecordMeta meta = result.getRecords().get(0);
        assertMeta(meta);

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(SOURCE_ID);
        RecordsQueryResult<RecordMeta> queryResult = recordsService.queryRecords(query, schema);

        assertEquals(1, queryResult.getRecords().size());
        assertMeta(queryResult.getRecords().get(0));
    }

    private void assertMeta(RecordMeta meta) {

        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisAsTest0").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisAsTest1").get("str").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisAsTest2").get("as").get("str").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisAsTest3").get("as").get("as").get("str").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisAsTest4").get("as").get("as").get("as").get("str").asText());

        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisEdgeTest1").get("val").get("str").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisEdgeTest2").get("val").get("edge").get("valAlias").get("str").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisEdgeTest3").get("val").get("edge").get("val").get("str").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisEdgeTest4").get("val").get("edge").get("val").get("edge").get("val").get("str").asText());
        assertEquals("this", meta.getAttribute("thisEdgeTest5").get("name").asText());

        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisEdgeTest6").get("val").get("edge").get("valValue").get("edge").get("val").get("str").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisEdgeTest6").get("val").get("edge").get("valValue").get("asStrAtt").get("str").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisEdgeTest6").get("val").get("edge").get("valValue").get("asThisInner").get("edge").get("val").get("str").asText());

        assertEquals("this", meta.getAttribute("thisEdgeTest6").get("val").get("edge").get("name").asText());
        assertEquals(MetaVal.STRING_VALUE, meta.getAttribute("thisEdgeTest6").get("val").get("edge").get("strVal").get("att").get("str").asText());

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

        public MetaVal() {
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
                case "this":
                    return this;
            }
            return null;
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            if (name.equals("date")) {
                return DATE_VALUE;
            }
            if (name.equals("this")) {
                return this;
            }
            if (name.equals("strAtt")) {
                return getString();
            }
            return null;
        }
    }
}
