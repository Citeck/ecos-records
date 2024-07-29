package ru.citeck.ecos.records2.test;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.api.promise.Promise;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdAttTest implements RecordAttsDao {

    private static final String ID = "test";

    private RecordsService recordsService;

    private final Map<String, Object> metaValues = new HashMap<>();

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        recordsService.register(this);

        EntityRef refTest = EntityRef.create(ID, "test");
        metaValues.put("test", new Value(refTest));
        metaValues.put("ValueByRef", new ValueByRef());
    }

    @Test
    void test() {
        DataValue attribute = recordsService.getAtt(EntityRef.create(ID, "test"), "id");
        assertEquals("test-id", attribute.asText());
    }

    @Test
    void refFieldTest() {

        EntityRef testRef = EntityRef.create(ID, "test");

        DataValue attribute = recordsService.getAtt(testRef, "otherRef?id");
        assertEquals(EntityRef.create(ID, ValueByRef.class.getSimpleName()).toString(), attribute.asText());

        attribute = recordsService.getAtt(testRef, "otherRef?str");
        assertEquals(EntityRef.create(ID, ValueByRef.class.getSimpleName()).toString(), attribute.asText());

        MetaClass meta = recordsService.getAtts(testRef, MetaClass.class);
        assertEquals(EntityRef.create(ID, ValueByRef.class.getSimpleName()), meta.getOtherRef());
        assertTrue(meta.initialized);
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {
        return metaValues.get(recordId);
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Data
    public static class MetaClass {
        private EntityRef otherRef;
        private Boolean initialized;
    }

    public static class ValueByRef implements AttValue {

        @Override
        public String asText() {
            return getClass().getName();
        }
    }

    public static class Value implements AttValue {

        private EntityRef ref;

        private boolean initialized;

        Value(EntityRef ref) {
            this.ref = ref;
        }


        @Nullable
        @Override
        public Promise<?> init() throws Exception {
            initialized = true;
            return null;
        }

        @Override
        public String getId() {
            return ref.getLocalId();
        }

        @Override
        public Object getAtt(String name) throws Exception {
            switch (name) {
                case "id": return "test-id";
                case "otherRef": return EntityRef.create(ID, ValueByRef.class.getSimpleName());
                case "initialized": return initialized;
            }
            return null;
        }
    }
}
