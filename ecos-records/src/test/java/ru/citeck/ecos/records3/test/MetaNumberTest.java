package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaNumberTest extends AbstractRecordsDao
                            implements RecordsAttsDao {

    RecordsService recordsService;

    @NotNull
    @Override
    public String getId() {
        return "test";
    }

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();

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
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return records.stream().map(TestValue::new).collect(Collectors.toList());
    }

    static class TestValue implements AttValue {

        private final RecordRef ref;

        public TestValue(String ref) {
            this.ref = RecordRef.create("test", ref);
        }

        @Override
        public String getId() {
            return ref.toString();
        }

        @Override
        public Double asDouble() {
            return 1_000_000_000.0;
        }
    }
}
