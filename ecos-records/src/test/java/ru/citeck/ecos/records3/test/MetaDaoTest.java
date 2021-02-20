package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaAttributesSupplier;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetaDaoTest extends AbstractRecordsDao implements RecordsAttsDao {

    private static final String ID = "123";

    private static final byte[] BYTES_STR = "one two three".getBytes(StandardCharsets.UTF_8);

    private RecordsService recordsService;

    private final Map<String, Object> metaAtts = Collections.singletonMap("key", "value");

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

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
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(this);
    }

    @Test
    void test() {

        RecordRef ref = RecordRef.valueOf("meta@");

        DataValue value = recordsService.getAtt(ref, "rec.123@VALUE.field");
        assertEquals("VALUE", value.asText());

        String attValue = recordsService.getAtt(ref, "attributes.key?str").asText();
        assertEquals(metaAtts.get("key"), attValue);

        String enumAttValue = recordsService.getAtt(ref, "rec.123@VALUE.enum").asText();
        assertEquals("FIRST", enumAttValue);

        String bytesValue = recordsService.getAtt(ref, "rec.123@VALUE.bytes").asText();
        assertEquals(Base64.getEncoder().encodeToString(BYTES_STR), bytesValue);
    }

    @Override
    public List<AttValue> getRecordsAtts(List<String> records) {
        return records.stream().map(RecordRef::valueOf).map(Value::new).collect(Collectors.toList());
    }

    public static class Value implements AttValue {

        private final RecordRef ref;

        Value(RecordRef ref) {
            this.ref = ref;
        }

        @Override
        public Object getAtt(@NotNull String name) {
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
