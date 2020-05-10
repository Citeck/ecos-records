package ru.citeck.ecos.records.test.objdata;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ObjectDataTest extends LocalRecordsDAO implements LocalRecordsMetaDAO<Object> {

    private static final String JSON = "{\"a\":\"b\"}";

    private static String ID = "";
    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        DataValue value = recordsService.getAttribute(RecordRef.valueOf("test"), "data?json");
        assertEquals("b", value.get("a").asText());

        DataValue value2 = recordsService.getAttribute(RecordRef.valueOf("test"), "data.a?str");
        assertEquals("b", value2.asText());
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream().map(TestData::new).collect(Collectors.toList());
    }

    @Data
    public static class TestData {

        private ObjectData data = ObjectData.create(Json.getMapper().read(JSON, Object.class));

        TestData(RecordRef ref) {
        }
    }
}
