package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;
import ru.citeck.ecos.records2.source.dao.remote.RecordsRestConnection;
import ru.citeck.ecos.records2.source.dao.remote.RemoteRecordsDAO;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoteRecordsDAOTest {

    private static final String REMOTE_SOURCE_ID = "remote";

    private RecordsService recordsService;
    private RestHandler queryHandler;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();

        recordsService = factory.getRecordsService();
        queryHandler = factory.getRestHandler();

        RemoteRecordsDAO remoteRecordsDAO = new RemoteRecordsDAO();
        remoteRecordsDAO.setId(REMOTE_SOURCE_ID);
        remoteRecordsDAO.setRestConnection(new RecordsRestConnection() {
            @Override
            public <T> T jsonPost(String url, Object request, Class<T> resultType) {
                return JsonUtils.convert(queryHandler.queryRecords((QueryBody) request), resultType);
            }
        });

        recordsService.register(new TestSource());
        recordsService.register(remoteRecordsDAO);
    }

    @Test
    void getAttributesTest() {

        Map<String, String> atts = new HashMap<>();
        atts.put("field", "field");
        RecordMeta attValues = recordsService.getAttributes(RecordRef.valueOf(REMOTE_SOURCE_ID + "@@"), atts);

        assertEquals(TestDto.FIELD_VALUE, attValues.get("field", ""));
    }

    public static class TestSource extends LocalRecordsDAO implements RecordsMetaLocalDAO<TestDto> {

        static final String ID = "";

        TestSource() {
            setId(ID);
        }

        @Override
        public List<TestDto> getMetaValues(List<RecordRef> records) {
            return records.stream()
                .map(r -> new TestDto(r.toString()))
                .collect(Collectors.toList());
        }
    }

    public static class TestDto {

        static final String FIELD_VALUE = "TestFieldValue";

        private String id;

        TestDto(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getField() {
            return FIELD_VALUE;
        }
    }
}
