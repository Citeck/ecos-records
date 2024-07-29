package ru.citeck.ecos.records2.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaAttributesSupplier;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetaDaoTest implements RecordAttsDao {

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
        recordsService = factory.getRecordsService();
        recordsService.register(this);
    }

    @Test
    void test() {

        EntityRef ref = EntityRef.valueOf("meta@");

        DataValue value = recordsService.getAtt(ref, "rec.123@VALUE.field");
        assertEquals("VALUE", value.asText());

        String attValue = recordsService.getAtt(ref, "attributes.key?str").asText();
        assertEquals(metaAtts.get("key"), attValue);

        String enumAttValue = recordsService.getAtt(ref, "rec.123@VALUE.enum").asText();
        assertEquals("FIRST", enumAttValue);

        String bytesValue = recordsService.getAtt(ref, "rec.123@VALUE.bytes").asText();
        assertEquals(Base64.getEncoder().encodeToString(BYTES_STR), bytesValue);
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {
        return new Value(EntityRef.create(getId(), recordId));
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    public static class Value implements AttValue {

        private EntityRef ref;

        Value(EntityRef ref) {
            this.ref = ref;
        }

        @Override
        public Object getAtt(String name) {
            if (name.equals("field")) {
                return ref.getLocalId();
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
