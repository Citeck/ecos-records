package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601Utils;
import org.junit.jupiter.api.*;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsServiceTests extends LocalRecordsDAO
                                 implements RecordsQueryLocalDAO,
                                            RecordsQueryWithMetaLocalDAO<Object>,
                                            RecordsMetaLocalDAO<Object> {

    private static final String SOURCE_ID = "test-source-id";

    private RecordsService recordsService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Override
    public List<Object> getMetaValues(List<RecordRef> records) {
        return records.stream().map(r -> new PojoMeta(r.toString())).collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery recordsQuery) {

        ExactIdsQuery query = recordsQuery.getQuery(ExactIdsQuery.class);

        RecordsQueryResult<RecordRef> result = new RecordsQueryResult<>();
        result.setRecords(query.getIds()
                               .stream()
                               .map(RecordRef::new)
                               .collect(Collectors.toList()));

        result.setHasMore(false);
        result.setTotalCount(query.getIds().size());

        return result;
    }

    @Override
    public RecordsQueryResult<Object> getMetaValues(RecordsQuery query) {
        return null;
    }

    @Test
    void testUnknownSourceQuery() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId("unknown-id");

        RecordsQueryResult<RecordRef> records = recordsService.getRecords(query);

        assertEquals(records.getRecords().size(), 0);
        assertEquals(records.getTotalCount(), 0);
        assertFalse(records.getHasMore());
    }

    @Test
    void testExactIdsQuery() throws JsonProcessingException {

        List<String> ids = Arrays.asList("Record0", "Record1", "Record2");

        ExactIdsQuery daoQuery = new ExactIdsQuery();
        daoQuery.setIds(ids);

        RecordsQuery query = new RecordsQuery();
        query.setQuery(objectMapper.writeValueAsString(daoQuery));
        query.setSourceId(SOURCE_ID);

        RecordsQueryResult<RecordRef> records = recordsService.getRecords(query);
        List<RecordRef> recordsRefs = records.getRecords();

        assertEquals(recordsRefs.size(), ids.size());
        assertEquals(records.getTotalCount(), ids.size());
        assertFalse(records.getHasMore());

        for (int i = 0; i < recordsRefs.size(); i++) {

            RecordRef ref = recordsRefs.get(i);
            assertEquals(SOURCE_ID, ref.getSourceId());
            assertEquals(ids.get(i), ref.getId());
        }
    }

    @Test
    void testSingleAttribute() {

        JsonNode value = recordsService.getAttribute(new RecordRef(SOURCE_ID, "TEST_REC_ID"), "fieldStr0");
        System.out.println(value);
    }

    @Test
    void testRecordsPojoMeta() {



    }

    @Test
    void testMetaQuery() {


    }
/*

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({
            "0,    1,   1",
            "1,    2,   3",
            "49,  51, 100",
            "1,  100, 101"
    })*/


    public static class PojoMeta {

        public static Integer TOTAL = 10;

        public static String STR_FIELD_0_POSTFIX = "str0";
        public static String STR_FIELD_1_POSTFIX = "str1";

        public static String MAP_FIELD_NAME = "mapField";
        public static String INNER_MAP_DATE_FIELD = "date";
        public static Date INNER_MAP_DATE_VALUE = ISO8601Utils.parse("2019-02-20T10:50:30Z");

        private String id;
        private String fieldStr0;
        private String fieldStr1;

        private Map<String, Map<String, Object>> fieldMap;

        public PojoMeta(String id) {

            this.fieldStr0 = id + STR_FIELD_0_POSTFIX;
            this.fieldStr1 = id + STR_FIELD_1_POSTFIX;

            fieldMap = new HashMap<>();

            Map<String, Object> innerMap = new HashMap<>();
            innerMap.put(INNER_MAP_DATE_FIELD, INNER_MAP_DATE_VALUE);
            fieldMap.put(MAP_FIELD_NAME, innerMap);
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

        public Map<String, Map<String, Object>> getFieldMap() {
            return fieldMap;
        }

        public void setFieldMap(Map<String, Map<String, Object>> fieldMap) {
            this.fieldMap = fieldMap;
        }
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
