package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaNumberTest extends LocalRecordsDao
                            implements LocalRecordsMetaDao {

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
        DataValue value = recordsService.getAtt(RecordRef.create("test", ""), ".num");
        String strValue = value.asText();
        //assertEquals("1000000000.0", strValue); //todo
    }

    @NotNull
    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records) {
        return records.stream().map(TestValue::new).collect(Collectors.toList());
    }

    static class TestValue implements AttValue {

        private final RecordRef ref;

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
