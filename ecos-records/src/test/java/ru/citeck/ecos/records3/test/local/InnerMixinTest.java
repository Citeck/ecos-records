package ru.citeck.ecos.records3.test.local;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAtt;
import ru.citeck.ecos.records3.record.atts.computed.RecordComputedAttType;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.mixin.AttMixin;
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.type.RecordTypeInfoAdapter;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InnerMixinTest extends AbstractRecordsDao
                            implements RecordsAttsDao {

    private static final String ID = "mixinSourceId";
    private static final EntityRef DTO_REC_REF = EntityRef.create(ID, "test");
    private static final EntityRef TEST_TYPE0 = EntityRef.create("TYPE", "TEST0");
    private static final EntityRef TEST_TYPE1 = EntityRef.create("TYPE", "TEST1");

    private RecordsService recordsService;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        factory.setRecordTypeComponent(type -> new RecordTypeInfoAdapter() {
            @NotNull
            public List<RecordComputedAtt> getComputedAtts() {
                if (type.equals(TEST_TYPE0)) {
                    RecordComputedAtt att = RecordComputedAtt.create()
                        .withId("computed")
                        .withType(RecordComputedAttType.ATTRIBUTE)
                        .withConfig(ObjectData.create("{\"attribute\":\"computedAtt0\"}"))
                        .build();
                    return Collections.singletonList(att);
                } else if (type.equals(TEST_TYPE1)) {
                    RecordComputedAtt att = RecordComputedAtt.create()
                        .withId("computed")
                        .withType(RecordComputedAttType.ATTRIBUTE)
                        .withConfig(ObjectData.create("{\"attribute\":\"computedAtt1\"}"))
                        .build();
                    return Collections.singletonList(att);
                }
                return Collections.emptyList();
            }
        });
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(this);

        addAttributesMixin(new AttMixin() {
            @Override
            public Object getAtt(String path, AttValueCtx value) {
                if (path.equals("*innerMeta")) {
                    return "innerValue";
                }
                return null;
            }
            @Override
            public Collection<String> getProvidedAtts() {
                return Collections.singletonList("*innerMeta");
            }
        });
    }

    @Test
    void test() {

        DataValue expected = DataValue.createStr("innerValue");

        DataValue innerRes = recordsService.getAtt(DTO_REC_REF, "innerMeta");
        assertEquals(expected, innerRes);

        innerRes = recordsService.getAtt(DTO_REC_REF, "inner.innerMeta");
        assertEquals(expected, innerRes);

        innerRes = recordsService.getAtt(DTO_REC_REF, "inner.inner.innerMeta");
        assertEquals(expected, innerRes);

        DataValue computedExpected0 = DataValue.createStr("computedValue0");
        DataValue computedExpected1 = DataValue.createStr("computedValue1");

        innerRes = recordsService.getAtt(DTO_REC_REF, "computed");
        assertEquals(computedExpected0, innerRes);

        innerRes = recordsService.getAtt(DTO_REC_REF, "inner.computed");
        assertEquals(computedExpected0, innerRes);

        innerRes = recordsService.getAtt(DTO_REC_REF, "inner.inner.computed");
        assertEquals(computedExpected1, innerRes);
    }

    @Override
    public List<?> getRecordsAtts(List<String> records) {
        return records.stream().map(ref -> {
            if (ref.equals(DTO_REC_REF.getLocalId())) {
                return new RecRefData(TEST_TYPE0, new RecRefData(TEST_TYPE0, new RecRefData(TEST_TYPE1)));
            }
            return EmptyAttValue.INSTANCE;
        }).collect(Collectors.toList());
    }

    @Data
    private static class RecRefData {

        private EntityRef type;
        private RecRefData inner;
        private String computedAtt0 = "computedValue0";
        private String computedAtt1 = "computedValue1";

        public RecRefData(EntityRef type) {
            this.type = type;
        }

        public RecRefData(EntityRef type, RecRefData inner) {
            this.inner = inner;
            this.type = type;
        }

        @AttName(".type")
        public EntityRef getType() {
            return type;
        }
    }
}
