package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.objdata.DataValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;

import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaNumberTest extends LocalRecordsDAO
                            implements RecordsMetaLocalDAO<Object> {

    RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId("test");
        recordsService.register(this);
    }

    @Test
    public void test() {
        DataValue value = recordsService.getAttribute(RecordRef.create("test", ""), ".num");
        String strValue = value.asText();
        //assertEquals("1000000000.0", strValue); //todo
    }

    @Override
    public List<Object> getMetaValues(List<RecordRef> records) {
        return records.stream().map(TestValue::new).collect(Collectors.toList());
    }

    class TestValue implements MetaValue {

        private RecordRef ref;

        public TestValue(RecordRef ref) {
            this.ref = ref;
        }

        @Override
        public String getId() {
            return ref.toString();
        }

        @Override
        public Double getDouble() {
            return 1_000_000_000.0;
        }
    }
}
