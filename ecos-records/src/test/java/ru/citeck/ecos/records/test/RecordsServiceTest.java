package ru.citeck.ecos.records.test;

import ecos.com.fasterxml.jackson210.core.JsonProcessingException;
import ecos.com.fasterxml.jackson210.databind.util.ISO8601Utils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.op.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.op.query.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.RecordsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsServiceTest extends AbstractRecordsDao
                                implements RecordsQueryDao,
                                           RecordsAttsDao {

    private static final String SOURCE_ID = "test-source-id";
    private static final RecordRef TEST_REF = RecordRef.create(SOURCE_ID, "TEST_REC_ID");

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @NotNull
    @Override
    public List<Object> getRecordsAtts(@NotNull List<String> records) {
        return records.stream().map(r -> new PojoMeta(r)).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public RecordsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {

        ExactIdsQuery query = recordsQuery.getQuery(ExactIdsQuery.class);

        RecordsQueryRes<Object> result = new RecordsQueryRes<>();
        result.setRecords(getRecordsAtts(query.getIds()));

        result.setHasMore(false);
        result.setTotalCount(query.getIds().size());

        return result;
    }

    @Test
    void testUnknownSourceQuery() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId("unknown-id");

        RecordsQueryRes<RecordRef> records = recordsService.query(query);

        assertEquals(0, records.getRecords().size());
        assertEquals(0, records.getTotalCount());
        assertFalse(records.getHasMore());
    }

    @Test
    void testExactIdsQuery() throws JsonProcessingException {

        List<String> ids = Arrays.asList("Record0", "Record1", "Record2");

        ExactIdsQuery daoQuery = new ExactIdsQuery();
        daoQuery.setIds(ids);

        RecordsQuery query = new RecordsQuery();
        query.setQuery(daoQuery);
        query.setSourceId(SOURCE_ID);

        RecordsQueryRes<RecordRef> records = recordsService.query(query);
        List<RecordRef> recordsRefs = records.getRecords();

        assertEquals(ids.size(), recordsRefs.size());
        assertEquals(ids.size(), records.getTotalCount());
        assertFalse(records.getHasMore());

        for (int i = 0; i < recordsRefs.size(); i++) {

            RecordRef ref = recordsRefs.get(i);
            assertEquals(SOURCE_ID, ref.getSourceId());
            assertEquals(ids.get(i), ref.getId());
        }
    }

    @Test
    void testSingleStrAttribute() {

        DataValue value = recordsService.getAtt(TEST_REF, "fieldStr0");

        DataValue expected = DataValue.create(TEST_REF.getId() + PojoMeta.STR_FIELD_0_POSTFIX);

        if (!expected.equals(value)) {
            String info = "(" + value.getClass().getName() + ") " + value;
            fail("Return value is incorrect: " + info);
        }
    }

    @Test
    void testSingleDateAttribute() {

        DataValue value = recordsService.getAtt(TEST_REF, "fieldDate");

        DataValue expected = DataValue.create(ISO8601Utils.format(PojoMeta.DATE_TEST_VALUE));

        if (!expected.equals(value)) {
            String info = "(" + value.getClass().getName() + ") " + value;
            fail("Return value is incorrect: " + info);
        }
    }

    @Test
    void testRecordsPojoMetaWithoutAnnotations() {

        PojoMetaModelWithoutAnnotations result = recordsService.getAtts(TEST_REF, PojoMetaModelWithoutAnnotations.class);
        PojoMeta pojoMeta = new PojoMeta(TEST_REF.getId());

        assertEquals(pojoMeta.getFieldDate(), result.getFieldDate());
        assertEquals(pojoMeta.getFieldStr0(), result.getFieldStr0());
        assertEquals(pojoMeta.getFieldStr1(), result.getFieldStr1());
    }

    @Test
    void testRecordsPojoMetaWithAnnotations() {

        PojoMetaWithAnnotations result = recordsService.getAtts(TEST_REF, PojoMetaWithAnnotations.class);
        PojoMeta pojoMeta = new PojoMeta(TEST_REF.getId());

        assertEquals(pojoMeta.getFieldDate(), result.getDate());
        assertEquals(pojoMeta.getFieldStr0(), result.getStr0());
        assertEquals(pojoMeta.getFieldStr1(), result.getStr1());
    }

    @Test
    void testRecordsPojoInnerFields() {

        PojoMetaInnerTest result = recordsService.getAtts(TEST_REF, PojoMetaInnerTest.class);
        PojoMeta pojoMeta = new PojoMeta(TEST_REF.getId());

        assertEquals(pojoMeta.getFieldStr0(), result.getStr0());

        Map<String, Object> fieldMap = pojoMeta.getFieldMap();
        PojoMetaInnerTest.ComplexInner actualFieldMap = result.getFieldMap();

        assertEquals(fieldMap.get("date"), actualFieldMap.getDate());
        assertEquals(fieldMap.get("str"), actualFieldMap.getStr());
    }

    @Test
    void testInnerPojo() {

        RecordsQuery query = new RecordsQuery();

        ExactIdsQuery exactIdsQuery = new ExactIdsQuery();
        exactIdsQuery.setIds(Collections.singletonList("list"));
        query.setQuery(exactIdsQuery);
        query.setSourceId(SOURCE_ID);

        RecordsQueryRes<JournalListInfo> records = recordsService.query(query, JournalListInfo.class);
        assertEquals(1, records.getTotalCount());

        List<JournalInfo> journals = records.getRecords().get(0).getJournals();
        assertEquals(2, journals.size());

        assertEquals("FirstName", journals.get(0).getName());
        assertEquals("FirstTitle", journals.get(0).getTitle());

        assertEquals("SecondName", journals.get(1).getName());
        assertEquals("SecondTitle", journals.get(1).getTitle());
    }

    @Test
    void testDisplayName() {

        DataValue dispValue = recordsService.getAtt(RecordRef.create(SOURCE_ID, "test"), ".disp");

        assertEquals(DataValue.create(PojoMeta.DISPLAY_NAME), dispValue);
    }

    @Test
    void testRootJsonAttribute() {

        DataValue value = recordsService.getAtt(RecordRef.create(SOURCE_ID, "test"), ".json");
        DataValue test = Json.getMapper().convert(new PojoMeta("test"), DataValue.class);

        assertEquals(test, value);
    }

    public static class JournalInfo {

        private String name;
        private String title;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static class JournalListInfo {

        private List<JournalInfo> journals;

        public List<JournalInfo> getJournals() {
            return journals;
        }

        public void setJournals(List<JournalInfo> journals) {
            this.journals = journals;
        }
    }

    public static class PojoMetaInnerTest {

        @AttName("fieldStr0")
        private String str0;
        private ComplexInner fieldMap;

        public String getStr0() {
            return str0;
        }

        public void setStr0(String str0) {
            this.str0 = str0;
        }

        public ComplexInner getFieldMap() {
            return fieldMap;
        }

        public void setFieldMap(ComplexInner fieldMap) {
            this.fieldMap = fieldMap;
        }

        static class ComplexInner {

            private Date date;
            private String str;

            public Date getDate() {
                return date;
            }

            public void setDate(Date date) {
                this.date = date;
            }

            public String getStr() {
                return str;
            }

            public void setStr(String str) {
                this.str = str;
            }
        }
    }

    public static class PojoMetaWithAnnotations {

        @AttName("fieldDate")
        private Date date;
        @AttName("fieldStr0")
        private String str0;
        @AttName("fieldStr1")
        private String str1;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getStr0() {
            return str0;
        }

        public void setStr0(String str0) {
            this.str0 = str0;
        }

        public String getStr1() {
            return str1;
        }

        public void setStr1(String str1) {
            this.str1 = str1;
        }
    }

    public static class PojoMetaModelWithoutAnnotations {

        private Date fieldDate;
        private String fieldStr0;
        private String fieldStr1;

        public Date getFieldDate() {
            return fieldDate;
        }

        public void setFieldDate(Date fieldDate) {
            this.fieldDate = fieldDate;
        }

        public String getFieldStr0() {
            return fieldStr0;
        }

        public void setFieldStr0(String fieldStr0) {
            this.fieldStr0 = fieldStr0;
        }

        public String getFieldStr1() {
            return fieldStr1;
        }

        public void setFieldStr1(String fieldStr1) {
            this.fieldStr1 = fieldStr1;
        }
    }

    public static class PojoMeta {

        static final String DISPLAY_NAME = "DISP_NAME_TEST";

        public static String STR_FIELD_0_POSTFIX = "str0";
        public static String STR_FIELD_1_POSTFIX = "str1";

        public static String MAP_FIELD_NAME = "mapField";
        public static String INNER_MAP_DATE_FIELD = "date";
        public static String INNER_MAP_STR_FIELD = "str";
        //milliseconds will be lost
        public static Date DATE_TEST_VALUE = new Date(21912321312000L);

        private String id;
        private String fieldStr0;
        private String fieldStr1;
        private Date fieldDate;

        private Map<String, Object> fieldMap;

        private List<Map<String, String>> journals;

        private SimpleObj simpleObjField = new SimpleObj();

        public PojoMeta(String id) {

            this.id = id;

            this.fieldStr0 = id + STR_FIELD_0_POSTFIX;
            this.fieldStr1 = id + STR_FIELD_1_POSTFIX;

            fieldMap = new HashMap<>();

            fieldMap = new HashMap<>();
            fieldMap.put(INNER_MAP_DATE_FIELD, DATE_TEST_VALUE);
            fieldMap.put(INNER_MAP_STR_FIELD, "TestInnerStrField");

            fieldDate = DATE_TEST_VALUE;

            journals = new ArrayList<>();
            Map<String, String> attributes = new HashMap<>();
            attributes.put("name", "FirstName");
            attributes.put("title", "FirstTitle");
            journals.add(attributes);

            attributes = new HashMap<>();
            attributes.put("name", "SecondName");
            attributes.put("title", "SecondTitle");
            journals.add(attributes);
        }

        @AttName(".disp")
        public String getDisplay() {
            return DISPLAY_NAME;
        }

        public SimpleObj getSimpleObjField() {
            return simpleObjField;
        }

        public List<Map<String, String>> getJournals() {
            return journals;
        }

        public Date getFieldDate() {
            return fieldDate;
        }

        public void setFieldDate(Date fieldDate) {
            this.fieldDate = fieldDate;
        }

        public String getId() {
            return id;
        }

        public String getFieldStr0() {
            return fieldStr0;
        }

        public void setFieldStr0(String fieldStr0) {
            this.fieldStr0 = fieldStr0;
        }

        public String getFieldStr1() {
            return fieldStr1;
        }

        public void setFieldStr1(String fieldStr1) {
            this.fieldStr1 = fieldStr1;
        }

        public Map<String, Object> getFieldMap() {
            return fieldMap;
        }

        public void setFieldMap(Map<String, Object> fieldMap) {
            this.fieldMap = fieldMap;
        }
    }

    public static class SimpleObj {
        public String field = "String";
    }

    public static class ExactIdsQuery {

        private List<String> ids;

        public List<String> getIds() {
            return ids;
        }

        public void setIds(List<String> ids) {
            this.ids = ids;
        }
    }
}
