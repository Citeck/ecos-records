package ru.citeck.ecos.records.test;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdAttTest extends AbstractRecordsDao implements RecordsAttsDao {

    private static final String ID = "test";

    private RecordsService recordsService;

    private final Map<RecordRef, Object> metaValues = new HashMap<>();

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);

        RecordRef refTest = RecordRef.create(ID, "test");
        metaValues.put(RecordRef.valueOf("test"), new Value(refTest));
        metaValues.put(RecordRef.valueOf("ValueByRef"), new ValueByRef());
    }

    @Test
    void test() {
        DataValue attribute = recordsService.getAtt(RecordRef.create(ID, "test"), "id");
        assertEquals("test-id", attribute.asText());
    }

    @Test
    void refFieldTest() {

        RecordRef testRef = RecordRef.create(ID, "test");

        DataValue attribute = recordsService.getAtt(testRef, "otherRef?str");
        assertEquals(RecordRef.create(ID, ValueByRef.class.getSimpleName()).toString(), attribute.asText());

        MetaClass meta = recordsService.getAtts(testRef, MetaClass.class);
        assertEquals(RecordRef.create(ID, ValueByRef.class.getSimpleName()), meta.getOtherRef());
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return records.stream().map(rec ->
            metaValues.get(RecordRef.create("", rec))).collect(Collectors.toList());
    }

    @Data
    public static class MetaClass {
        private RecordRef otherRef;
    }

    public static class ValueByRef implements AttValue {

        @Override
        public String getString() {
            return getClass().getName();
        }
    }

    public static class Value implements AttValue {

        private RecordRef ref;

        Value(RecordRef ref) {
            this.ref = ref;
        }

        @Override
        public String getId() {
            return ref.getId();
        }

        @Override
        public Object getAtt(@NotNull String name) throws Exception {
            switch (name) {
                case "id": return "test-id";
                case "otherRef": return RecordRef.create(ID, ValueByRef.class.getSimpleName());
            }
            return null;
        }
    }
}
