package ru.citeck.ecos.records.test;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.rest.QueryBody;
import ru.citeck.ecos.records3.rest.RestHandler;

import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.source.dao.remote.RecordsRestConnection;
import ru.citeck.ecos.records3.source.dao.remote.RemoteRecordsDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoteRecordsDaoTest {

    private static final String REMOTE_SOURCE_ID = "remote";

    private RecordsService recordsService;
    private RestHandler queryHandler;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();

        recordsService = factory.getRecordsService();
        queryHandler = factory.getRestHandler();

        RemoteRecordsDao remoteRecordsDao = new RemoteRecordsDao();
        //remoteRecordsDao.setId(REMOTE_SOURCE_ID);
        remoteRecordsDao.setRestConnection(new RecordsRestConnection() {
            @Override
            public <T> T jsonPost(String url, Object request, Class<T> resultType) {
                return Json.getMapper().convert(queryHandler.queryRecords((QueryBody) request), resultType);
            }
        });

        recordsService.register(new TestSource());
        //recordsService.register(remoteRecordsDao);
    }

    @Test
    void getAttributesTest() {

        Map<String, String> atts = new HashMap<>();
        atts.put("field", "field");
        atts.put("data", "data?json");
        //RecordAtts attValues = recordsService.getAtts(RecordRef.valueOf(REMOTE_SOURCE_ID + "@@"), atts);

        //assertEquals(TestDto.FIELD_VALUE, attValues.get("field", ""));
        //assertEquals(TestDto.OBJ_DATA_VALUE.getData(), attValues.get("data"));
    }

    public static class TestSource extends AbstractRecordsDao implements RecordsAttsDao {

        static final String ID = "";

        TestSource() {
            setId(ID);
        }

        @NotNull
        @Override
        public List<TestDto> getRecordsAtts(@NotNull List<String> records) {
            return records.stream()
                .map(r -> new TestDto(r.toString()))
                .collect(Collectors.toList());
        }
    }

    public static class TestDto {

        static final String FIELD_VALUE = "TestFieldValue";
        static final ObjectData OBJ_DATA_VALUE = ObjectData.create("{\"a\":\"b\"}");

        private String id;

        @Getter
        private final ObjectData data = OBJ_DATA_VALUE;

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
