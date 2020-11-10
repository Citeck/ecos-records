package ru.citeck.ecos.records2.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaNumberTest extends LocalRecordsDao
                            implements LocalRecordsMetaDao<Object> {

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
        DataValue value = recordsService.getAttribute(RecordRef.create("test", ""), "double1_000_000_000");
        String strValue = value.asText();
        assertEquals("1000000000", strValue);
        value = recordsService.getAttribute(RecordRef.create("test", ""), "double2_000_000_000\\.0123458");
        strValue = value.asText();
        assertEquals("2000000000.0123458", strValue);
    }

    @NotNull
    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
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

        @Override
        public Object getAttribute(@NotNull String name, @NotNull MetaField field) throws Exception {
            switch (name) {
                case "double1_000_000_000":
                    return 1_000_000_000d;
                case "double2_000_000_000.0123458":
                    return 2_000_000_000.0123458d;
            }
            return null;
        }
    }
}
