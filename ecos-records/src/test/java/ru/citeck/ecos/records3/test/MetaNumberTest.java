package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        recordsService = factory.getRecordsService();

        recordsService.register(this);
    }

    @Test
    public void test() {
        DataValue value = recordsService.getAtt(EntityRef.create("test", ""), "double1_000_000_000");
        String strValue = value.asText();
        assertEquals("1000000000", strValue);
        value = recordsService.getAtt(EntityRef.create("test", ""), "double2_000_000_000\\.0123458");
        strValue = value.asText();
        assertEquals("2000000000.0123458", strValue);
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return records.stream().map(TestValue::new).collect(Collectors.toList());
    }

    static class TestValue implements AttValue {

        private final EntityRef ref;

        public TestValue(String ref) {
            this.ref = EntityRef.create("test", ref);
        }

        @Override
        public String getId() {
            return ref.toString();
        }

        @Override
        public Double asDouble() {
            return 1_000_000_000.0;
        }

        @Nullable
        @Override
        public Object getAtt(String name) throws Exception {
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
