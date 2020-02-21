package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.objdata.DataValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetaDAOTest extends LocalRecordsDAO implements LocalRecordsMetaDAO<MetaValue> {

    private static final String ID = "123";
    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        setId(ID);
        recordsService = factory.getRecordsService();
        recordsService.register(this);
    }

    @Test
    void test() {

        RecordRef ref = RecordRef.valueOf("meta@");

        DataValue value = recordsService.getAttribute(ref, "rec.123@VALUE.field");
        assertEquals("VALUE", value.asText());
    }

    @Override
    public List<MetaValue> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream().map(Value::new).collect(Collectors.toList());
    }

    public static class Value implements MetaValue {

        private RecordRef ref;

        Value(RecordRef ref) {
            this.ref = ref;
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            if (name.equals("field")) {
                return ref.getId();
            }
            return null;
        }
    }
}
