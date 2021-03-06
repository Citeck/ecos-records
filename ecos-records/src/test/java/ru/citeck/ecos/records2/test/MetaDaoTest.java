package ru.citeck.ecos.records2.test;

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
import ru.citeck.ecos.records2.source.dao.local.meta.MetaAttributesSupplier;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetaDaoTest extends LocalRecordsDao implements LocalRecordsMetaDao<MetaValue> {

    private static final String ID = "123";

    private static final byte[] BYTES_STR = "one two three".getBytes(StandardCharsets.UTF_8);

    private RecordsService recordsService;

    private final Map<String, Object> metaAtts = Collections.singletonMap("key", "value");

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        factory.getMetaRecordsDaoAttsProvider().register(new MetaAttributesSupplier() {
            @Override
            public List<String> getAttributesList() {
                return new ArrayList<>(metaAtts.keySet());
            }
            @Override
            public Object getAttribute(String attribute) throws Exception {
                return metaAtts.get(attribute);
            }
        });
        setId(ID);
        recordsService = factory.getRecordsService();
        recordsService.register(this);
    }

    @Test
    void test() {

        RecordRef ref = RecordRef.valueOf("meta@");

        DataValue value = recordsService.getAttribute(ref, "rec.123@VALUE.field");
        assertEquals("VALUE", value.asText());

        String attValue = recordsService.getAttribute(ref, "attributes.key?str").asText();
        assertEquals(metaAtts.get("key"), attValue);

        String enumAttValue = recordsService.getAttribute(ref, "rec.123@VALUE.enum").asText();
        assertEquals("FIRST", enumAttValue);

        String bytesValue = recordsService.getAttribute(ref, "rec.123@VALUE.bytes").asText();
        assertEquals(Base64.getEncoder().encodeToString(BYTES_STR), bytesValue);
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
            } else if (name.equals("enum")) {
                return TestEnum.FIRST;
            } else if (name.equals("bytes")) {
                return BYTES_STR;
            }
            return null;
        }
    }

    public enum TestEnum { FIRST, SECOND }
}
