package ru.citeck.ecos.records.test.objdata;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ObjectDataAndDataValueTest extends AbstractRecordsDao implements RecordsAttsDao {

    private static final String JSON = "{\"a\":\"b\"}";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        setId("");
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        recordsService.register(this);
    }

    @Test
    void test() {

        DataValue value = recordsService.getAtt(RecordRef.valueOf("test"), "data?json");
        assertEquals("b", value.get("a").asText());

        DataValue value2 = recordsService.getAtt(RecordRef.valueOf("test"), "data.a?str");
        assertEquals("b", value2.asText());

        value = recordsService.getAtt(RecordRef.valueOf("test"), "dataValue?json");
        assertEquals("b", value.get("a").asText());

        value2 = recordsService.getAtt(RecordRef.valueOf("test"), "dataValue.a?str");
        assertEquals("b", value2.asText());

        value2 = recordsService.getAtt(RecordRef.valueOf("test"), "data.unknown?str");
        assertEquals(DataValue.NULL, value2);

        value2 = recordsService.getAtt(RecordRef.valueOf("test"), "dataValue.unknown?str");
        assertEquals(DataValue.NULL, value2);

        value2 = recordsService.getAtt(RecordRef.valueOf("test"), "unknown?str");
        assertEquals(DataValue.NULL, value2);
    }

    @Test
    void dtoTest() {

        TestDataMeta dtoValue = recordsService.getAtts(RecordRef.valueOf("test"), TestDataMeta.class);
        TestDataMeta expected = new TestDataMeta(new TestData("test"));
        assertEquals(expected, dtoValue);
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return records.stream()
            .map(TestData::new)
            .collect(Collectors.toList());
    }

    @Data
    public static class TestData {

        private ObjectData data = ObjectData.create(Json.getMapper().read(JSON, Object.class));
        private DataValue dataValue = DataValue.create(JSON);

        TestData(String ref) {
        }
    }

    @Data
    public static class TestDataMeta {

        private ObjectData data;
        private DataValue dataValue;

        public TestDataMeta(TestData data) {
            this.data = data.data;
            this.dataValue = data.dataValue;
        }

        public TestDataMeta() {
        }
    }
}
