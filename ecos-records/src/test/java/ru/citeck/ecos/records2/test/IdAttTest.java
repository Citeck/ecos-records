package ru.citeck.ecos.records2.test;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdAttTest extends LocalRecordsDao implements LocalRecordsMetaDao<Object> {

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
        DataValue attribute = recordsService.getAttribute(RecordRef.create(ID, "test"), "id");
        assertEquals("test-id", attribute.asText());
    }

    @Test
    void refFieldTest() {

        RecordRef testRef = RecordRef.create(ID, "test");

        DataValue attribute = recordsService.getAttribute(testRef, "otherRef?id");
        assertEquals(RecordRef.create(ID, ValueByRef.class.getSimpleName()).toString(), attribute.asText());

        attribute = recordsService.getAttribute(testRef, "otherRef?str");
        assertEquals(RecordRef.create(ID, ValueByRef.class.getSimpleName()).toString(), attribute.asText());

        MetaClass meta = recordsService.getMeta(testRef, MetaClass.class);
        assertEquals(RecordRef.create(ID, ValueByRef.class.getSimpleName()), meta.getOtherRef());
        assertTrue(meta.initialized);
    }

    @NotNull
    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
        return records.stream()
            .map(metaValues::get)
            .collect(Collectors.toList());
    }

    @Data
    public static class MetaClass {
        private RecordRef otherRef;
        private Boolean initialized;
    }

    public static class ValueByRef implements MetaValue {

        @Override
        public String getString() {
            return getClass().getName();
        }
    }

    public static class Value implements MetaValue {

        private RecordRef ref;

        private boolean initialized;

        Value(RecordRef ref) {
            this.ref = ref;
        }

        @Override
        public <T extends QueryContext> void init(@NotNull T context, @NotNull MetaField field) {
            initialized = true;
        }

        @Override
        public String getId() {
            return ref.getId();
        }

        @Override
        public Object getAttribute(String name, MetaField field) throws Exception {
            switch (name) {
                case "id": return "test-id";
                case "otherRef": return RecordRef.create(ID, ValueByRef.class.getSimpleName());
                case "initialized": return initialized;
            }
            return null;
        }
    }
}
